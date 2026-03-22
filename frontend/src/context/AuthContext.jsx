import { createContext, useContext, useState } from 'react';

const AuthContext = createContext(null);

export function AuthProvider({ children }) {
    const [token, setToken] = useState(localStorage.getItem('token'));

    const login = (newToken) => {
        console.log("Auth Context: token: ",newToken);
        localStorage.setItem('token', newToken);
        setToken(newToken);
    };

    const logout = () => {
        localStorage.removeItem('token');
        setToken(null);
    };

    const isLoggedIn = !!token;

    return (
        <AuthContext.Provider value={{ token, login, logout, isLoggedIn }}>
            {children}
        </AuthContext.Provider>
    );
}

export function useAuth() {
    return useContext(AuthContext);
}