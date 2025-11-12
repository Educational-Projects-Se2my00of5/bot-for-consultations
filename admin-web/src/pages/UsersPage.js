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
        const token = localStorage.getItem('adminToken');
        try {
            const response = await fetch(`/api/admin/activate-account/${userId}`, {
                headers: {
                    'Authorization': `Bearer ${token}`
                }
            });

            if (response.ok) {
                alert('Аккаунт преподавателя успешно активирован!');
                setIsModalOpen(false);
                fetchUsers();
            } else {
                alert('Ошибка при активации аккаунта');
            }
        } catch (error) {
            console.error('Ошибка:', error);
            alert('Ошибка при активации аккаунта');
        }
    };

    useEffect(() => {
        fetchUsers();
    }, []);

    if (loading) {
        return (
            <div className="users-page">
                <Header />
                <div className="loading">Загрузка...</div>
            </div>
        );
    }

    return (
        <div className="users-page">
            <Header />
            <div className="users-container">
                <div className="users-header">
                    <h2>Список неактивных преподавателей</h2>
                    <div className="header-actions">
                        <button 
                            className="nav-button"
                            onClick={() => window.location.href = '/deanery'}
                        >
                            Деканат
                        </button>
                    </div>
                </div>
                
                {error && <div className="error">{error}</div>}
                
                {users.length === 0 ? (
                    <div className="empty-state">
                        <p>Нет неактивных преподавателей</p>
                    </div>
                ) : (
                    <div className="users-grid">
                        {users.map(user => (
                            <div 
                                key={user.id} 
                                className="user-card" 
                                onClick={() => getUserDetails(user.id)}
                            >
                                <h3>{(user.firstName + " " + ((user.lastName != null) ? user.lastName : "")) || 'Без имени'}</h3>
                                <p>Телефон: {user.phone || 'Не указан'}</p>
                            </div>
                        ))}
                    </div>
                )}
            </div>

            <Modal 
                isOpen={isModalOpen} 
                onClose={() => setIsModalOpen(false)}
            >
                {selectedUser && (
                    <div className="user-details">
                        <h2>Информация о преподавателе</h2>
                        <div className="detail-row">
                            <strong>Имя:</strong>
                            <span>{selectedUser.firstName || 'Не указано'}</span>
                        </div>
                        <div className="detail-row">
                            <strong>Фамилия:</strong>
                            <span>{selectedUser.lastName || 'Не указана'}</span>
                        </div>
                        <div className="detail-row">
                            <strong>Отчество:</strong>
                            <span>{selectedUser.patronymic || 'Не указано'}</span>
                        </div>
                        <div className="detail-row">
                            <strong>Телефон:</strong>
                            <span>{selectedUser.phone || 'Не указан'}</span>
                        </div>
                        <div className="detail-row">
                            <strong>Email:</strong>
                            <span>{selectedUser.email || 'Не указан'}</span>
                        </div>
                        <div className="detail-row">
                            <strong>Telegram ID:</strong>
                            <span>{selectedUser.telegramId}</span>
                        </div>
                        <div className="modal-actions">
                            <button 
                                className="activate-button"
                                onClick={() => activateUser(selectedUser.id)}
                            >
                                Активировать
                            </button>
                            <button 
                                className="cancel-button"
                                onClick={() => setIsModalOpen(false)}
                            >
                                Отмена
                            </button>
                        </div>
                    </div>
                )}
            </Modal>
        </div>
    );
};

export default UsersPage;