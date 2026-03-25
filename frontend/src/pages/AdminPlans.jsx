import { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import api from '../services/api';

const emptyForm = { name: '', description: '', price: '', durationInDays: '' };

export default function AdminPlans() {
    const [plans, setPlans]       = useState([]);
    const [form, setForm]         = useState(emptyForm);
    const [loading, setLoading]   = useState(true);
    const [submitting, setSubmitting] = useState(false);
    const [deletingId, setDeletingId] = useState(null);
    const [error, setError]       = useState('');
    const [formError, setFormError] = useState('');
    const [success, setSuccess]   = useState('');

    const { logout } = useAuth();
    const navigate   = useNavigate();

    useEffect(() => {
        fetchPlans();
    }, []);

    const fetchPlans = async () => {
        setLoading(true);
        try {
            const res = await api.get('/plans?page=0&size=50');
            setPlans(res.data.content || []);
        } catch {
            setError('Failed to load plans');
        } finally {
            setLoading(false);
        }
    };

    const handleChange = (e) => {
        setForm({ ...form, [e.target.name]: e.target.value });
        setFormError('');
    };

    const handleCreate = async () => {
        // Client-side validation
        if (!form.name.trim()) {
            setFormError('Plan name is required');
            return;
        }
        if (!form.price || isNaN(form.price) || Number(form.price) < 0) {
            setFormError('Enter a valid price');
            return;
        }
        if (!form.durationInDays || isNaN(form.durationInDays) || Number(form.durationInDays) < 1) {
            setFormError('Duration must be at least 1 day');
            return;
        }

        setSubmitting(true);
        setFormError('');
        setSuccess('');

        try {
            await api.post('/plans', {
                name:          form.name.trim(),
                description:   form.description.trim(),
                price:         Number(form.price),
                durationInDays: Number(form.durationInDays),
            });
            setSuccess(`Plan "${form.name}" created successfully!`);
            setForm(emptyForm);
            fetchPlans(); // refresh list
        } catch (err) {
            const data = err.response?.data;
            setFormError(
                data?.errors?.join(', ') ||
                data?.error ||
                'Failed to create plan'
            );
        } finally {
            setSubmitting(false);
        }
    };

    const handleDelete = async (planId, planName) => {
        if (!window.confirm(`Delete plan "${planName}"? This cannot be undone.`)) return;
        setDeletingId(planId);
        try {
            await api.delete(`/plans/${planId}`);
            setPlans(prev => prev.filter(p => p.id !== planId));
            setSuccess(`Plan "${planName}" deleted.`);
        } catch (err) {
            setError(err.response?.data?.error || 'Delete failed');
        } finally {
            setDeletingId(null);
        }
    };

    return (
        <div style={styles.page}>

            {/* Header */}
            <div style={styles.header}>
                <div>
                    <h1 style={styles.title}>Plan Management</h1>
                    <p style={styles.subtitle}>Admin dashboard</p>
                </div>
                <button
                    style={styles.logoutBtn}
                    onClick={() => { logout(); navigate('/login'); }}
                >
                    Logout
                </button>
            </div>

            {/* Global error */}
            {error && <p style={styles.globalError}>{error}</p>}

            <div style={styles.layout}>

                {/* LEFT — Create plan form */}
                <div style={styles.formCard}>
                    <h2 style={styles.cardTitle}>Create New Plan</h2>

                    <label style={styles.label}>Plan name *</label>
                    <input
                        style={styles.input}
                        name="name"
                        placeholder="e.g. Pro, Basic, Enterprise"
                        value={form.name}
                        onChange={handleChange}
                    />

                    <label style={styles.label}>Description</label>
                    <textarea
                        style={{ ...styles.input, height: '80px', resize: 'vertical' }}
                        name="description"
                        placeholder="What's included in this plan?"
                        value={form.description}
                        onChange={handleChange}
                    />

                    <div style={styles.row}>
                        <div style={{ flex: 1 }}>
                            <label style={styles.label}>Price (₹) *</label>
                            <input
                                style={styles.input}
                                name="price"
                                type="number"
                                min="0"
                                placeholder="99"
                                value={form.price}
                                onChange={handleChange}
                            />
                        </div>
                        <div style={{ flex: 1 }}>
                            <label style={styles.label}>Duration (days) *</label>
                            <input
                                style={styles.input}
                                name="durationInDays"
                                type="number"
                                min="1"
                                placeholder="30"
                                value={form.durationInDays}
                                onChange={handleChange}
                            />
                        </div>
                    </div>

                    {formError && <p style={styles.error}>{formError}</p>}
                    {success   && <p style={styles.success}>{success}</p>}

                    <button
                        style={styles.createBtn}
                        onClick={handleCreate}
                        disabled={submitting}
                    >
                        {submitting ? 'Creating...' : 'Create Plan'}
                    </button>
                </div>

                {/* RIGHT — Existing plans list */}
                <div style={styles.listCard}>
                    <h2 style={styles.cardTitle}>
                        Existing Plans
                        <span style={styles.badge}>{plans.length}</span>
                    </h2>

                    {loading && <p style={styles.muted}>Loading plans...</p>}

                    {!loading && plans.length === 0 && (
                        <p style={styles.muted}>No plans yet. Create your first plan.</p>
                    )}

                    <div style={styles.planList}>
                        {plans.map(plan => (
                            <div key={plan.id} style={styles.planRow}>
                                <div style={styles.planInfo}>
                                    <span style={styles.planName}>{plan.name}</span>
                                    {plan.description && (
                                        <span style={styles.planDesc}>{plan.description}</span>
                                    )}
                                    <span style={styles.planMeta}>
                    ₹{plan.price} · {plan.durationInDays} days
                  </span>
                                </div>
                                <button
                                    style={styles.deleteBtn}
                                    onClick={() => handleDelete(plan.id, plan.name)}
                                    disabled={deletingId === plan.id}
                                >
                                    {deletingId === plan.id ? '...' : 'Delete'}
                                </button>
                            </div>
                        ))}
                    </div>
                </div>
            </div>
        </div>
    );
}

const styles = {
    page: {
        maxWidth: '1000px',
        margin: '0 auto',
        padding: '2rem',
    },
    header: {
        display: 'flex',
        justifyContent: 'space-between',
        alignItems: 'flex-start',
        marginBottom: '2rem',
        borderBottom: '1px solid #e5e7eb',
        paddingBottom: '1rem',
    },
    title: {
        fontSize: '22px',
        fontWeight: '600',
        margin: '0 0 4px',
        color: '#111',
    },
    subtitle: {
        fontSize: '13px',
        color: '#9ca3af',
        margin: 0,
    },
    logoutBtn: {
        padding: '6px 14px',
        background: '#ef4444',
        color: '#fff',
        border: 'none',
        borderRadius: '6px',
        cursor: 'pointer',
        fontSize: '13px',
    },
    globalError: {
        background: '#fee2e2',
        color: '#dc2626',
        padding: '10px 14px',
        borderRadius: '6px',
        fontSize: '14px',
        marginBottom: '16px',
    },
    layout: {
        display: 'grid',
        gridTemplateColumns: '380px 1fr',
        gap: '24px',
        alignItems: 'start',
    },
    formCard: {
        background: '#fff',
        border: '1px solid #e5e7eb',
        borderRadius: '10px',
        padding: '1.5rem',
        boxShadow: '0 1px 4px rgba(0,0,0,0.06)',
    },
    listCard: {
        background: '#fff',
        border: '1px solid #e5e7eb',
        borderRadius: '10px',
        padding: '1.5rem',
        boxShadow: '0 1px 4px rgba(0,0,0,0.06)',
    },
    cardTitle: {
        fontSize: '16px',
        fontWeight: '600',
        margin: '0 0 18px',
        color: '#111',
        display: 'flex',
        alignItems: 'center',
        gap: '8px',
    },
    badge: {
        background: '#f3f4f6',
        color: '#6b7280',
        fontSize: '12px',
        fontWeight: '600',
        padding: '2px 8px',
        borderRadius: '10px',
    },
    label: {
        display: 'block',
        fontSize: '13px',
        fontWeight: '500',
        color: '#374151',
        marginBottom: '4px',
        marginTop: '12px',
    },
    input: {
        display: 'block',
        width: '100%',
        padding: '9px 12px',
        border: '1px solid #d1d5db',
        borderRadius: '6px',
        fontSize: '14px',
        boxSizing: 'border-box',
        outline: 'none',
        fontFamily: 'inherit',
    },
    row: {
        display: 'flex',
        gap: '12px',
        marginTop: '4px',
    },
    createBtn: {
        marginTop: '18px',
        width: '100%',
        padding: '10px',
        background: '#4f46e5',
        color: '#fff',
        border: 'none',
        borderRadius: '6px',
        fontSize: '14px',
        fontWeight: '500',
        cursor: 'pointer',
    },
    error: {
        color: '#dc2626',
        fontSize: '13px',
        marginTop: '8px',
    },
    success: {
        color: '#16a34a',
        fontSize: '13px',
        marginTop: '8px',
    },
    muted: {
        color: '#9ca3af',
        fontSize: '14px',
    },
    planList: {
        display: 'flex',
        flexDirection: 'column',
        gap: '10px',
    },
    planRow: {
        display: 'flex',
        justifyContent: 'space-between',
        alignItems: 'center',
        padding: '12px 14px',
        background: '#f9fafb',
        borderRadius: '8px',
        border: '1px solid #e5e7eb',
    },
    planInfo: {
        display: 'flex',
        flexDirection: 'column',
        gap: '2px',
    },
    planName: {
        fontWeight: '600',
        fontSize: '14px',
        color: '#111',
    },
    planDesc: {
        fontSize: '12px',
        color: '#6b7280',
    },
    planMeta: {
        fontSize: '13px',
        color: '#4f46e5',
        fontWeight: '500',
    },
    deleteBtn: {
        padding: '5px 12px',
        background: 'transparent',
        color: '#ef4444',
        border: '1px solid #fca5a5',
        borderRadius: '6px',
        cursor: 'pointer',
        fontSize: '12px',
        flexShrink: 0,
    },
};