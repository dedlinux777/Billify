import { useState } from 'react';
import { useNavigate, Link } from 'react-router-dom';
import api from '../services/api';

export default function Register() {
    const [form, setForm]     = useState({ name:'', email:'', password:'' });
    const [error, setError]   = useState('');
    const [loading, setLoading] = useState(false);
    const navigate = useNavigate();

    const handleChange = (e) => {
        setForm({ ...form, [e.target.name]: e.target.value });
    };

    const handleSubmit = async () => {
        setError('');
        setLoading(true);
        try {
            await api.post('/auth/register', form);
            navigate('/login');
        } catch (err) {
            const data = err.response?.data;
            setError(data?.errors?.join(', ') || data?.error || 'Registration failed');
        } finally {
            setLoading(false);
        }
    };

    return (
        <div style={styles.container}>
            <div style={styles.card}>
                <h2 style={styles.title}>Create Account</h2>
                <p style={styles.subtitle}>Join Billify today</p>

                <input style={styles.input} name="name"
                       placeholder="Full name" value={form.name} onChange={handleChange} />
                <input style={styles.input} name="email" type="email"
                       placeholder="Email" value={form.email} onChange={handleChange} />
                <input style={styles.input} name="password" type="password"
                       placeholder="Password (min 6 chars)" value={form.password} onChange={handleChange} />

                {error && <p style={styles.error}>{error}</p>}

                <button style={styles.button} onClick={handleSubmit} disabled={loading}>
                    {loading ? 'Creating account...' : 'Create Account'}
                </button>
                <p style={styles.link}>
                    Already have an account? <Link to="/login">Sign in</Link>
                </p>
            </div>
        </div>
    );
}

const styles = {
    container: { display:'flex', justifyContent:'center',
        alignItems:'center', minHeight:'100vh', background:'#f5f5f5' },
    card: { background:'#fff', padding:'2rem', borderRadius:'8px',
        boxShadow:'0 2px 12px rgba(0,0,0,0.1)', width:'360px' },
    title: { margin:'0 0 4px', fontSize:'24px', fontWeight:'600' },
    subtitle: { margin:'0 0 24px', color:'#666', fontSize:'14px' },
    input: { display:'block', width:'100%', padding:'10px 12px',
        marginBottom:'12px', border:'1px solid #ddd', borderRadius:'6px',
        fontSize:'14px', boxSizing:'border-box' },
    button: { width:'100%', padding:'10px', background:'#4f46e5',
        color:'#fff', border:'none', borderRadius:'6px', fontSize:'15px', cursor:'pointer' },
    error: { color:'#dc2626', fontSize:'13px', marginBottom:'8px' },
    link: { textAlign:'center', marginTop:'16px', fontSize:'13px', color:'#666' },
};