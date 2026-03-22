import { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import api from '../services/api';

export default function MySubscription() {
    const [subscription, setSubscription] = useState(null);
    const [plans, setPlans]               = useState([]);
    const [loading, setLoading]           = useState(true);
    const [actionLoading, setActionLoading] = useState(false);
    const [error, setError]               = useState('');
    const [message, setMessage]           = useState('');

    const navigate = useNavigate();

    useEffect(() => {
        fetchData();
    }, []);

    const fetchData = async () => {
        setLoading(true);
        try {
            const [subRes, plansRes] = await Promise.all([
                api.get('/subscriptions/my').catch(() => ({ data: null })),
                api.get('/plans?page=0&size=20'),
            ]);
            setSubscription(subRes.data);
            setPlans(plansRes.data.content || []);
        } catch (err) {
            setError('Failed to load data');
        } finally {
            setLoading(false);
        }
    };

    const handleCancel = async () => {
        if (!window.confirm('Cancel your subscription?')) return;
        setActionLoading(true);
        try {
            await api.put('/subscriptions/cancel');
            setMessage('Subscription cancelled.');
            setSubscription(null);
        } catch (err) {
            setError(err.response?.data?.error || 'Cancel failed');
        } finally {
            setActionLoading(false);
        }
    };

    const handleUpgrade = async (planId) => {
        setActionLoading(true);
        setMessage('');
        setError('');
        try {
            const res = await api.put(`/subscriptions/upgrade/${planId}`);
            setSubscription(res.data);
            setMessage('Upgraded successfully!');
        } catch (err) {
            setError(err.response?.data?.error || 'Upgrade failed');
        } finally {
            setActionLoading(false);
        }
    };

    if (loading) return <div style={styles.page}><p>Loading...</p></div>;

    return (
        <div style={styles.page}>
            <div style={styles.header}>
                <h1 style={styles.title}>My Subscription</h1>
                <div style={{ display:'flex', gap:'12px' }}>
                    <button style={styles.linkBtn} onClick={() => navigate('/plans')}>
                        View Plans
                    </button>
                    <button style={styles.linkBtn} onClick={() => navigate('/billing')}>
                        Billing History
                    </button>
                </div>
            </div>

            {error   && <p style={styles.error}>{error}</p>}
            {message && <p style={styles.success}>{message}</p>}

            {!subscription ? (
                <div style={styles.empty}>
                    <p>You have no active subscription.</p>
                    <button style={styles.button} onClick={() => navigate('/plans')}>
                        Browse Plans
                    </button>
                </div>
            ) : (
                <div style={styles.card}>
                    <div style={styles.badge}>ACTIVE</div>
                    <h2 style={styles.planName}>{subscription.planName}</h2>
                    <p style={styles.detail}>₹{subscription.planPrice}</p>
                    <p style={styles.detail}>
                        Started: {new Date(subscription.startDate).toLocaleDateString()}
                    </p>
                    <p style={styles.detail}>
                        Expires: {new Date(subscription.endDate).toLocaleDateString()}
                    </p>

                    <button
                        style={styles.cancelBtn}
                        onClick={handleCancel}
                        disabled={actionLoading}
                    >
                        {actionLoading ? 'Processing...' : 'Cancel Subscription'}
                    </button>

                    <h3 style={styles.upgradeTitle}>Upgrade to another plan</h3>
                    <div style={styles.upgradeList}>
                        {plans
                            .filter(p => p.name !== subscription.planName)
                            .map(plan => (
                                <div key={plan.id} style={styles.upgradeItem}>
                                    <span>{plan.name} — ₹{plan.price}</span>
                                    <button
                                        style={styles.upgradeBtn}
                                        onClick={() => handleUpgrade(plan.id)}
                                        disabled={actionLoading}
                                    >
                                        Upgrade
                                    </button>
                                </div>
                            ))}
                    </div>
                </div>
            )}
        </div>
    );
}

const styles = {
    page: { maxWidth:'700px', margin:'0 auto', padding:'2rem' },
    header: { display:'flex', justifyContent:'space-between',
        alignItems:'center', marginBottom:'2rem' },
    title: { fontSize:'24px', fontWeight:'600', margin:0 },
    linkBtn: { background:'none', border:'none', color:'#4f46e5',
        cursor:'pointer', fontSize:'14px', textDecoration:'underline' },
    card: { background:'#fff', border:'1px solid #e5e7eb', borderRadius:'8px',
        padding:'2rem', boxShadow:'0 1px 4px rgba(0,0,0,0.06)' },
    badge: { display:'inline-block', background:'#dcfce7', color:'#16a34a',
        padding:'2px 10px', borderRadius:'12px', fontSize:'12px',
        fontWeight:'600', marginBottom:'12px' },
    planName: { fontSize:'22px', fontWeight:'600', margin:'0 0 12px' },
    detail: { color:'#6b7280', fontSize:'14px', margin:'4px 0' },
    cancelBtn: { marginTop:'16px', padding:'8px 20px', background:'#ef4444',
        color:'#fff', border:'none', borderRadius:'6px', cursor:'pointer' },
    upgradeTitle: { marginTop:'28px', fontSize:'16px', fontWeight:'600' },
    upgradeList: { marginTop:'12px', display:'flex', flexDirection:'column', gap:'10px' },
    upgradeItem: { display:'flex', justifyContent:'space-between',
        alignItems:'center', padding:'10px 14px', background:'#f9fafb',
        borderRadius:'6px', border:'1px solid #e5e7eb' },
    upgradeBtn: { padding:'5px 14px', background:'#4f46e5', color:'#fff',
        border:'none', borderRadius:'6px', cursor:'pointer', fontSize:'13px' },
    empty: { textAlign:'center', padding:'3rem', color:'#6b7280' },
    button: { padding:'8px 20px', background:'#4f46e5', color:'#fff',
        border:'none', borderRadius:'6px', cursor:'pointer', marginTop:'12px' },
    error: { color:'#dc2626', marginBottom:'12px' },
    success: { color:'#16a34a', marginBottom:'12px' },
};