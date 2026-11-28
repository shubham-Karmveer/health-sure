import type { NextConfig } from "next";

const isStaticExport = process.env.NEXT_OUTPUT === "export";

const nextConfig: NextConfig = {
  output: isStaticExport ? "export" : undefined,
  cacheComponents: !isStaticExport,
  partialPrefetching: !isStaticExport,
};

export default nextConfig;
