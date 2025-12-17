import React, { useEffect, useState } from 'react';
import { getInactiveUsers, activateUser, deleteUser } from '../api/adminApi';
import Header from '../components/Header';
import './InactiveUsersPage.css';

// Модалка для неактивного пользователя
const InactiveUserModal = ({ user, onClose, onActivate, onDelete }) => {
  const [isDeleting, setIsDeleting] = useState(false);

  const handleActivate = async () => {
    try {
      await onActivate(user.id);
      onClose();
    } catch (error) {
      alert(error.message);
    }
  };

  const handleDelete = async () => {
    if (!window.confirm(`Вы уверены, что хотите удалить ${user.firstName} ${user.lastName}?`)) {
      return;
    }

    setIsDeleting(true);
    try {
      await onDelete(user.id);
      onClose();
    } catch (error) {
      alert(error.message);
    } finally {
      setIsDeleting(false);
    }
  };

  return (
    <div className="modal-overlay" onClick={onClose}>
      <div className="modal-content" onClick={(e) => e.stopPropagation()}>
        <h2>Информация о пользователе</h2>
        
        <div className="user-details">
          <div className="detail-row">
            <strong>ID:</strong>
            <span>{user.id}</span>
          </div>
          <div className="detail-row">
            <strong>Имя:</strong>
            <span>{user.firstName}</span>
          </div>
          <div className="detail-row">
            <strong>Фамилия:</strong>
            <span>{user.lastName}</span>
          </div>
          <div className="detail-row">
            <strong>Телефон:</strong>
            <span>{user.phone || 'Не указан'}</span>
          </div>
          <div className="detail-row">
            <strong>Telegram ID:</strong>
            <span>{user.telegramId || 'Не указан'}</span>
          </div>
          <div className="detail-row">
            <strong>Роль:</strong>
            <span>{user.role}</span>
          </div>
        </div>

        <div className="modal-actions">
          <button className="activate-button" onClick={handleActivate}>
            Активировать
          </button>
          <button 
            className="delete-button" 
            onClick={handleDelete}
            disabled={isDeleting}
          >
            {isDeleting ? 'Удаление...' : 'Удалить'}
          </button>
          <button className="cancel-button" onClick={onClose}>
            Закрыть
          </button>
        </div>
      </div>
    </div>
  );
};

function InactiveUsersPage() {
  const [users, setUsers] = useState([]);
  const [filteredUsers, setFilteredUsers] = useState([]);
  const [selectedUser, setSelectedUser] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [roleFilter, setRoleFilter] = useState('all');

  useEffect(() => {
    loadUsers();
  }, []);

  useEffect(() => {
    let filtered = users;

    if (roleFilter !== 'all') {
      filtered = filtered.filter(user => user.role === roleFilter);
    }

    setFilteredUsers(filtered);
  }, [users, roleFilter]);

  const loadUsers = async () => {
    try {
      setLoading(true);
      const data = await getInactiveUsers();
      setUsers(data);
    } catch (err) {
      setError(err.message);
    } finally {
      setLoading(false);
    }
  };

  const handleActivate = async (id) => {
    await activateUser(id);
    await loadUsers();
  };

  const handleDelete = async (id) => {
    await deleteUser(id);
    await loadUsers();
  };

  return (
    <>
      <Header />
      <div className="main">
        {loading ? (
          <div className="loading">Загрузка...</div>
        ) : error ? (
          <div className="error">{error}</div>
        ) : (
          <div className="page-layout">
            <div className="left-panel">
              <div className="panel-header">
                <h2>Неактивные пользователи</h2>
                <span className="count">{filteredUsers.length}</span>
              </div>

              <div className="filters-section">
                <div className="filter-group">
                  <label htmlFor="role-filter">Фильтр по роли:</label>
                  <select
                    id="role-filter"
                    className="filter-select"
                    value={roleFilter}
                    onChange={(e) => setRoleFilter(e.target.value)}
                  >
                    <option value="all">Все роли</option>
                    <option value="TEACHER">Преподаватели</option>
                    <option value="DEANERY">Деканат</option>
                    <option value="STUDENT">Студенты</option>
                  </select>
                </div>
              </div>
            </div>

            <div className="right-panel">
              {filteredUsers.length === 0 ? (
                <div className="empty-state">
                  <p>Нет неактивных пользователей</p>
                </div>
              ) : (
                <div className="users-grid">
                  {filteredUsers.map((user) => (
                    <div
                      key={user.id}
                      className="user-card"
                      onClick={() => setSelectedUser(user)}
                    >
                      <h3>{user.firstName} {user.lastName}</h3>
                      <p><strong>Роль:</strong> {user.role}</p>
                      <p><strong>Телефон:</strong> {user.phone || 'Не указан'}</p>
                      <span className="status-badge inactive">Неактивен</span>
                    </div>
                  ))}
                </div>
              )}
            </div>
          </div>
        )}
      </div>

      {selectedUser && (
        <InactiveUserModal
          user={selectedUser}
          onClose={() => setSelectedUser(null)}
          onActivate={handleActivate}
          onDelete={handleDelete}
        />
      )}
    </>
  );
}

export default InactiveUsersPage;
