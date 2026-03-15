import { Outlet } from 'react-router-dom';
import { Sidebar } from './Sidebar';
import { Titlebar } from './Titlebar';

export function Layout() {
  return (
    <div className="app-root">
      <Titlebar />
      <div className="layout">
        <Sidebar />
        <main className="main-content">
          <div className="page-content">
            <Outlet />
          </div>
        </main>
      </div>
    </div>
  );
}
