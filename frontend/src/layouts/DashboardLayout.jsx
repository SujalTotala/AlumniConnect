import Sidebar from "../components/Sidebar";
import Navbar from "../components/Navbar";

function DashboardLayout({ children }) {
  return (
    <div className="flex min-h-screen bg-slate-50">
      {/* Fixed Sidebar */}
      <Sidebar />

      {/* Main Content Area offset by Sidebar width */}
      <div className="ml-64 flex-1 flex flex-col min-h-screen">
        <Navbar />
        <main className="p-8 flex-1">
          {children}
        </main>
      </div>
    </div>
  );
}

export default DashboardLayout;