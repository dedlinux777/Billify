import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom';
import { AuthProvider } from './context/AuthContext';
import PrivateRoute from './components/PrivateRoute';
import AdminRoute from './components/AdminRoute';

import Login from './pages/Login';
import Register from './pages/Register';
import Plans from './pages/Plans';
import MySubscription from './pages/MySubscription';
import BillingHistory from './pages/BillingHistory';
import AdminPlans from './pages/AdminPlans';

export default function App() {
  return (
      <AuthProvider>
        <BrowserRouter>
          <Routes>
            <Route path="/" element={<Navigate to="/login" replace />} />
            <Route path="/login" element={<Login />} />
            <Route path="/register" element={<Register />} />

            <Route path="/plans" element={
              <PrivateRoute><Plans /></PrivateRoute>
            } />
            <Route path="/subscription" element={
              <PrivateRoute><MySubscription /></PrivateRoute>
            } />
            <Route path="/billing" element={
              <PrivateRoute><BillingHistory /></PrivateRoute>
            } />

            {/* Admin routes */}
            <Route path="/admin/plans" element={
              <AdminRoute><AdminPlans /></AdminRoute>
            } />
          </Routes>
        </BrowserRouter>
      </AuthProvider>
  );
}