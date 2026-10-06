import { Navigate, Route, Routes } from 'react-router-dom';
import { AuthProvider } from './auth/AuthContext';
import { ProtectedRoute } from './components/ProtectedRoute';
import { Layout } from './components/Layout';
import { LoginPage } from './pages/LoginPage';
import { RegisterPage } from './pages/RegisterPage';
import { OrganizationsPage } from './pages/OrganizationsPage';
import { ProjectsPage } from './pages/ProjectsPage';
import { ProjectDetailPage } from './pages/ProjectDetailPage';
import { PipelineDetailPage } from './pages/PipelineDetailPage';

export default function App() {
  return (
    <AuthProvider>
      <Routes>
        <Route path="/login" element={<LoginPage />} />
        <Route path="/register" element={<RegisterPage />} />

        <Route element={<ProtectedRoute />}>
          <Route element={<Layout />}>
            <Route path="/organizations" element={<OrganizationsPage />} />
            <Route path="/organizations/:organizationId/projects" element={<ProjectsPage />} />
            <Route path="/projects/:projectId" element={<ProjectDetailPage />} />
            <Route path="/projects/:projectId/pipelines/:pipelineId" element={<PipelineDetailPage />} />
          </Route>
        </Route>

        <Route path="/" element={<Navigate to="/organizations" replace />} />
        <Route path="*" element={<Navigate to="/organizations" replace />} />
      </Routes>
    </AuthProvider>
  );
}
