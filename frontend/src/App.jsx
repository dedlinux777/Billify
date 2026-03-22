import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom';
import { AuthProvider } from './context/AuthContext';
import PrivateRoute from './components/PrivateRoute';

import Login from './pages/Login';
import Register from './pages/Register';
import Plans from './pages/Plans';
import MySubscription from './pages/MySubscription';
import BillingHistory from './pages/BillingHistory';

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
          </Routes>
        </BrowserRouter>
      </AuthProvider>
  );
}