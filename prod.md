# Production deployment runbook

This runbook deploys the existing HealthDekho application while keeping the already-provisioned PostgreSQL RDS schema and data unchanged:

- **Frontend:** Next.js static export in a private S3 bucket, delivered by CloudFront.
- **Backend:** Spring Boot executable JAR on Elastic Beanstalk Java 21.
- **Database:** the existing PostgreSQL RDS instance.
- **AI:** Amazon Bedrock Converse, accessed through the Elastic Beanstalk EC2 instance profile.

## Production and schema constraints

- The `prod` Spring profile **does not run Flyway**. Do not baseline, migrate, or clean the production database. The repository's older Flyway files are not the production schema contract.
- Hibernate uses `ddl-auto: validate`: startup checks the mappings but never creates or updates tables. If validation fails, stop and compare the database with `schema 1.sql`; do not suppress validation to force deployment.
- The backend maps the supplied plural tables and `BIGINT` IDs: `customers`, `insurance_products`, `customer_policies`, `premium_installments`, `premium_payments`, and `policy_recommendations`. `insurance_claims` remains outside the current API.
- **Do not run `schema 1.sql` or `data 1.sql` against production.** They are reference/sample scripts, not a deployment migration; the data script adds sample rows and the schema script includes a constraint alteration.
- Seeders are disabled in production. Existing customer, product, policy, installment, payment, and recommendation rows are used as-is.
- The supplied `customers` table has no password hash or account status. Production login is therefore restricted to the single customer email configured by `DEMO_LOGIN_EMAIL`, and only when the submitted password exactly matches `DEMO_LOGIN_PASSWORD`. This is a verified credential, not a password bypass. Other customer rows cannot sign in until a supported credential store is introduced.
- The schema stores recommendation results as flat rows, not request metadata. Recommendation inputs such as preferred term/type and model/prompt versions are not persisted. A response with no recommendations has no persisted history ID.
- The schema has no payment-idempotency table or cached response. The backend uses the existing `receipt_number` to recognize a repeated idempotency key and rejects reuse with a different payload. Because the schema cannot store a response snapshot, a replay's policy totals reflect current persisted payments.
- Use HTTPS custom domains under the same registrable domain (for example, `app.example.com` and `api.example.com`). The browser uses credentialed, secure, `SameSite=Lax` cookies; default S3/CloudFront and Elastic Beanstalk hostnames are not a suitable final cookie setup.
- Never put database credentials, demo passwords, JWT secrets, AWS access keys, or other secrets in Git, the JAR, frontend build variables, or a checked-in `.env` file.

## 1. Verify the existing RDS database

1. Confirm PostgreSQL engine/major version, database name, endpoint, port, VPC, and security group. RDS should remain private.
2. Take and verify a recoverable RDS snapshot before deployment.
3. Using an authorized SQL client with TLS, verify the expected tables exist:

   ```sql
   SELECT table_name
   FROM information_schema.tables
   WHERE table_schema = current_schema()
     AND table_name IN (
       'customers', 'insurance_products', 'customer_policies',
       'premium_installments', 'premium_payments',
       'policy_recommendations', 'insurance_claims'
     )
   ORDER BY table_name;
   ```

4. Compare their columns, identity types, precision, nullability, constraints, and foreign keys with `schema 1.sql`. Confirm that the configured demo email resolves to exactly one existing row in `customers.email`, and that active products and the expected customer data are already present.
5. Do not continue if a table or required column is missing, IDs are not compatible `BIGINT` values, or existing rows violate the supplied schema. Correct the deployment target or arrange a separately reviewed database change; do not make an application-side fallback.

## 2. Configure network access

1. Place Elastic Beanstalk in the VPC that can reach RDS, preferably in the same AWS Region. Configure a load balancer and application instances in suitable subnets.
2. Allow inbound TCP `5432` on the **RDS security group** only from the Elastic Beanstalk EC2 instance security group. Do not allow `0.0.0.0/0`.
3. Provide the EB instances outbound access to the required AWS services through NAT or VPC endpoints, including Bedrock and Secrets Manager.
4. Configure HTTPS and the custom API domain on the Elastic Beanstalk load balancer.

## 3. Create secrets and IAM permissions

Create Secrets Manager values for:

- `DATABASE_USERNAME`
- `DATABASE_PASSWORD`
- `JWT_SECRET` — a random secret with at least 32 bytes.
- `DEMO_LOGIN_PASSWORD` — a strong, unique demo credential. Share it only with the intended demo employee.

Set `DEMO_LOGIN_EMAIL` to the exact email of an existing row in `customers`. Store it in Secrets Manager as well if your environment policy requires it.

Attach least-privilege permissions to the Elastic Beanstalk **EC2 instance profile**:

- Read only the required secret ARNs (`secretsmanager:GetSecretValue`); include `kms:Decrypt` for the specific customer-managed key, if applicable.
- Allow `bedrock:InvokeModel` for the configured model/inference-profile resources.
- Keep the standard Elastic Beanstalk permissions required for health reporting and logs.

Configure secret values in Elastic Beanstalk as Secrets Manager references, not plaintext. Do not enable either seeder.

## 4. Build and deploy the backend

1. Run backend checks against an **isolated test database** created from the supplied schema—not the production RDS:

   ```powershell
   mvn -f backend/pom.xml clean verify
   ```

   The integration suite needs PostgreSQL with the supplied schema available. Never point tests or a developer workstation at production.

2. Package the executable JAR:

   ```powershell
   mvn -f backend/pom.xml clean package
   ```

3. Create an Elastic Beanstalk Java 21 environment on Amazon Linux 2023. Set its EC2 instance profile, VPC, security groups, and HTTPS listener.
4. Configure these Elastic Beanstalk environment properties:

   | Property | Value |
   |---|---|
   | `SPRING_PROFILES_ACTIVE` | `prod` |
   | `PORT` | `5000` |
   | `DATABASE_URL` | `jdbc:postgresql://<RDS-endpoint>:5432/<database>?sslmode=require` |
   | `DATABASE_USERNAME` | Secrets Manager reference |
   | `DATABASE_PASSWORD` | Secrets Manager reference |
   | `JWT_SECRET` | Secrets Manager reference |
   | `DEMO_LOGIN_EMAIL` | Existing `customers.email` value configured for the demo |
   | `DEMO_LOGIN_PASSWORD` | Secrets Manager reference |
   | `FRONTEND_ORIGIN` | Exact HTTPS frontend origin, e.g. `https://app.example.com` |
   | `AWS_REGION` | Region for the Bedrock model/inference profile |
   | `AI_PROVIDER` | `bedrock` |
   | `BEDROCK_MODEL_ID` | Model/inference profile enabled for the account and Region |
   | `SEED_CUSTOMER_ENABLED` | `false` |
   | `SEED_CATALOG_ENABLED` | `false` |

   `sslmode=require` encrypts the connection. For certificate and hostname verification, configure the current RDS CA certificate and PostgreSQL JDBC `sslmode=verify-full`; verify this in staging before production.

5. Upload the JAR as an Elastic Beanstalk application version and deploy. Keep the executable JAR at the root of any source bundle.
6. Wait for the environment to become healthy and check:

   ```text
   https://api.example.com/actuator/health
   ```

   Confirm Hibernate schema validation and database connectivity succeeded. Flyway must not appear as a production startup migration step.

## 5. Build and deploy the frontend

1. Create a dedicated S3 bucket with Block Public Access enabled. Configure CloudFront with the bucket as a private origin using Origin Access Control (OAC).
2. Request/validate the frontend ACM certificate in **US East (N. Virginia), `us-east-1`**, then configure the CloudFront alternate domain and DNS alias.
3. From PowerShell, build and verify the static export:

   ```powershell
   $env:NEXT_OUTPUT = "export"
   $env:NEXT_PUBLIC_API_BASE_URL = "https://api.example.com"
   npm --prefix frontend ci
   npm --prefix frontend test
   npm --prefix frontend run lint
   npm --prefix frontend run typecheck
   npm --prefix frontend run build
   ```

   Verify `frontend/out` contains the expected exported pages before uploading. `NEXT_PUBLIC_API_BASE_URL` is public browser configuration, not a secret.

4. Attach a CloudFront Function to the distribution's **viewer-request** event so clean paths resolve to their exported HTML:

   ```javascript
   function handler(event) {
     var request = event.request;
     var uri = request.uri;

     if (uri === "/") {
       request.uri = "/index.html";
     } else if (uri.endsWith("/")) {
       request.uri = uri.slice(0, -1) + ".html";
     } else if (!uri.split("/").pop().includes(".")) {
       request.uri = uri + ".html";
     }

     return request;
   }
   ```

   Set the CloudFront default root object to `index.html`; leave paths with file extensions unchanged.

5. Upload the export and invalidate CloudFront:

   ```powershell
   aws s3 sync .\frontend\out\ s3://<static-site-bucket>/
   aws cloudfront create-invalidation --distribution-id <distribution-id> --paths "/*"
   ```

   Avoid `--delete` unless the bucket is confirmed to contain only this application's generated site files.

## 6. Verify the deployment

1. Confirm frontend and backend custom domains use HTTPS and share the same parent domain.
2. Verify `FRONTEND_ORIGIN` exactly matches the frontend origin, including scheme and hostname, without a trailing slash.
3. Verify login using the configured demo email and exact secret password. Confirm a wrong password and a different existing customer's email both receive `401`; no arbitrary-password login is supported.
4. Verify the dashboard, available products, policy registration, policy lookup, installments, payment history/submission, and Bedrock recommendations against the existing RDS data.
5. Confirm direct refresh works on frontend routes and the browser has no CORS, mixed-content, blocked-cookie, or API base URL errors.
6. Verify application logs show schema validation success and no database or Bedrock permission errors. Do not include passwords, tokens, or database credentials in logs or support messages.

## Rollback and operations

- Retain the previous Elastic Beanstalk application version and the verified RDS snapshot. An application rollback does not roll back database changes.
- Keep the SQL reference/data scripts out of the production deployment workflow. The production application neither seeds rows nor runs schema changes.
- The frontend signup page is informational; the backend has no customer signup endpoint.
- The payment endpoint records payment/accounting rows only; it is not a payment-gateway integration.
