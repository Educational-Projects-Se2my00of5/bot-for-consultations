import React, {useEffect, useState} from 'react';
import Header from '../components/Header';
import Modal from '../components/Modal';
import './UsersPage.css';

const UsersPage = () => {
    const [users, setUsers] = useState([]);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState(null);
    const [selectedUser, setSelectedUser] = useState(null);
    const [isModalOpen, setIsModalOpen] = useState(false);

    const fetchUsers = async () => {
        setLoading(true);
        try {
            const response = await fetch('/api/admin/unactive-accounts', {
                headers: {
                    'Authorization': `Bearer ${localStorage.getItem('adminToken')}`
                }
            });

            if (!response.ok) {
                throw new Error('Ошибка при получении списка пользователей');
            }

            const data = await response.json();
            setUsers(data);
        } catch (err) {
            setError(err.message);
        } finally {
            setLoading(false);
        }
    };

    const getUserDetails = async (userId) => {
        try {
            const response = await fetch(`/api/admin/user-info/${userId}`, {
                headers: {
                    'Authorization': `Bearer ${localStorage.getItem('adminToken')}`
                }
            });

            if (!response.ok) {
                throw new Error('Ошибка при получении информации о пользователе');
            }

            const data = await response.json();
            setSelectedUser(data);
            setIsModalOpen(true);
        } catch (err) {
            setError(err.message);
        }
    };

    const activateUser = async (userId) => {
        try {
            const response = await fetch(`/api/admin/activate-account/${userId}`, {
                headers: {
                    'Authorization': `Bearer ${localStorage.getItem('adminToken')}`
                }
            });

            if (!response.ok) {
                throw new Error('Ошибка при активации пользователя');
            }

            setIsModalOpen(false);
            await fetchUsers(); // Обновляем список пользователей
        } catch (err) {
            setError(err.message);
        }
    };

    useEffect(() => {
        fetchUsers();
    }, []);

    if (loading) {
        return (
            <div className="page">
                <Header/>
                <main className="main">
                    <div className="loading">Загрузка...</div>
                </main>
            </div>
        );
    }

    return (
        <div className="page">
            <Header/>
            <main className="main">
                {error && <div className="error">{error}</div>}

                <div className="users-container">
                    <h2>Список неактивных преподавателей</h2>
                    {users.length === 0 ? (
                        <p>Нет неактивных преподавателей</p>
                    ) : (
                        <div className="users-grid">
                            {users.map(user => (
                                <div key={user.id} className="user-card" onClick={() => getUserDetails(user.id)}>
                                    <h3>{(user.firstName + " " + ((user.lastName != null) ? user.lastName : "")) || 'Без имени'}</h3>
                                    <p>Телефон: {user.phone || 'Не указан'}</p>
                                </div>
                            ))}
                        </div>
                    )}
                </div>

                <Modal isOpen={isModalOpen} onClose={() => setIsModalOpen(false)}>
                    {selectedUser && (
                        <div className="user-details">
                            <div className="modal-header">
                                <h2>Информация о пользователе</h2>
                                <button className="modal-close" onClick={() => setIsModalOpen(false)}>&times;</button>
                            </div>
                            <div className="user-info">
                                <p><strong>Имя:</strong> {selectedUser.firstName || 'Не указано'}</p>
                                <p><strong>Фамилия:</strong> {selectedUser.lastName || 'Не указана'}</p>
                                <p><strong>Телефон:</strong> {selectedUser.phone || 'Не указан'}</p>
                                <p><strong>ID:</strong> {selectedUser.id}</p>
                            </div>
                            <button
                                className="activate-button"
                                onClick={() => activateUser(selectedUser.id)}
                            >
                                Активировать
                            </button>
                        </div>
                    )}
                </Modal>
            </main>
        </div>
    );
};

export default UsersPage;