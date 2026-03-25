import { createContext, useContext, useState } from 'react';

const AuthContext = createContext(null);

// Decode JWT payload without a library — it's just base64
function parseRole(token) {
    try {
        const payload = JSON.parse(atob(token.split('.')[1]));
        return payload.role || null;
    } catch {
        return null;
    }
}

export function AuthProvider({ children }) {
    const [token, setToken] = useState(localStorage.getItem('token'));
    const [role, setRole]   = useState(() => {
        const t = localStorage.getItem('token');
        return t ? parseRole(t) : null;
    });

    const login = (newToken) => {
        localStorage.setItem('token', newToken);
        setToken(newToken);
        setRole(parseRole(newToken));
    };

    const logout = () => {
        localStorage.removeItem('token');
        setToken(null);
        setRole(null);
    };

    const isLoggedIn = !!token;
    const isAdmin    = role === 'ADMIN';

    return (
        <AuthContext.Provider value={{ token, login, logout, isLoggedIn, isAdmin, role }}>
            {children}
        </AuthContext.Provider>
    );
}

export function useAuth() {
    return useContext(AuthContext);
}