import Dashboard from "@/features/dashboard/Dashboard";
import SignedInShell from "@/components/SignedInShell";

export default function DashboardPage() {
  return (
    <SignedInShell pageTitle="Your dashboard">
      <Dashboard />
    </SignedInShell>
  );
}
