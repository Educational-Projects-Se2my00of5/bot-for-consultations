import React, { useEffect, useState } from 'react';
import { getActiveUsers, deactivateUser, updateUser, deleteUser } from '../api/adminApi';
import Header from '../components/Header';
import './ActiveUsersPage.css';

// Модалка для активного пользователя
const ActiveUserModal = ({ user, onClose, onDeactivate, onUpdate, onDelete }) => {
  const [isEditing, setIsEditing] = useState(false);
  const [formData, setFormData] = useState({
    firstName: user.firstName,
    lastName: user.lastName,
  });
  const [isSubmitting, setIsSubmitting] = useState(false);

  const handleDeactivate = async () => {
    try {
      await onDeactivate(user.id);
      onClose();
    } catch (error) {
      alert(error.message);
    }
  };

  const handleDelete = async () => {
    if (!window.confirm(`Вы уверены, что хотите удалить ${user.firstName} ${user.lastName}?`)) {
      return;
    }

    try {
      await onDelete(user.id);
      onClose();
    } catch (error) {
      alert(error.message);
    }
  };

  const handleUpdate = async () => {
    setIsSubmitting(true);
    try {
      await onUpdate(user.id, formData);
      setIsEditing(false);
      onClose();
    } catch (error) {
      alert(error.message);
    } finally {
      setIsSubmitting(false);
    }
  };

  const canDeactivate = user.role !== 'STUDENT';

  return (
    <div className="modal-overlay" onClick={onClose}>
      <div className="modal-content" onClick={(e) => e.stopPropagation()}>
        <h2>{isEditing ? 'Редактирование пользователя' : 'Информация о пользователе'}</h2>
        
        {!isEditing ? (
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
        ) : (
          <div className="edit-form">
            <label>
              Имя:
              <input
                type="text"
                value={formData.firstName}
                onChange={(e) => setFormData({ ...formData, firstName: e.target.value })}
              />
            </label>
            <label>
              Фамилия:
              <input
                type="text"
                value={formData.lastName}
                onChange={(e) => setFormData({ ...formData, lastName: e.target.value })}
              />
            </label>
          </div>
        )}

        <div className="modal-actions">
          {!isEditing ? (
            <>
              <button className="edit-button" onClick={() => setIsEditing(true)}>
                Редактировать
              </button>
              {canDeactivate && (
                <button className="deactivate-button" onClick={handleDeactivate}>
                  Деактивировать
                </button>
              )}
              <button className="delete-button" onClick={handleDelete}>
                Удалить
              </button>
              <button className="cancel-button" onClick={onClose}>
                Закрыть
              </button>
            </>
          ) : (
            <>
              <button 
                className="save-button" 
                onClick={handleUpdate}
                disabled={isSubmitting}
              >
                {isSubmitting ? 'Сохранение...' : 'Сохранить'}
              </button>
              <button 
                className="cancel-button" 
                onClick={() => setIsEditing(false)}
                disabled={isSubmitting}
              >
                Отмена
              </button>
            </>
          )}
        </div>
      </div>
    </div>
  );
};

function ActiveUsersPage() {
  const [users, setUsers] = useState([]);
  const [filteredUsers, setFilteredUsers] = useState([]);
  const [selectedUser, setSelectedUser] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [searchQuery, setSearchQuery] = useState('');
  const [roleFilter, setRoleFilter] = useState('all');

  useEffect(() => {
    loadUsers();
  }, []);

  useEffect(() => {
    let filtered = users;

    // Фильтр по роли
    if (roleFilter !== 'all') {
      filtered = filtered.filter(user => user.role === roleFilter);
    }

    // Поиск по имени, фамилии, телефону или telegram ID
    if (searchQuery) {
      const query = searchQuery.toLowerCase();
      filtered = filtered.filter(user => 
        user.firstName.toLowerCase().includes(query) ||
        user.lastName.toLowerCase().includes(query) ||
        (user.phone && user.phone.toLowerCase().includes(query)) ||
        (user.telegramId && user.telegramId.toLowerCase().includes(query))
      );
    }

    setFilteredUsers(filtered);
  }, [users, searchQuery, roleFilter]);

  const loadUsers = async () => {
    try {
      setLoading(true);
      const data = await getActiveUsers();
      setUsers(data);
    } catch (err) {
      setError(err.message);
    } finally {
      setLoading(false);
    }
  };

  const handleDeactivate = async (id) => {
    await deactivateUser(id);
    await loadUsers();
  };

  const handleUpdate = async (id, data) => {
    await updateUser(id, data);
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
                <h2>Активные пользователи</h2>
                <span className="count">{filteredUsers.length}</span>
              </div>

              <div className="filters-section">
                <input
                  type="text"
                  className="search-input"
                  placeholder="Поиск по имени, фамилии, телефону или Telegram ID..."
                  value={searchQuery}
                  onChange={(e) => setSearchQuery(e.target.value)}
                />
                
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
                  <p>Пользователи не найдены</p>
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
                      <span className="status-badge active">Активен</span>
                    </div>
                  ))}
                </div>
              )}
            </div>
          </div>
        )}
      </div>

      {selectedUser && (
        <ActiveUserModal
          user={selectedUser}
          onClose={() => setSelectedUser(null)}
          onDeactivate={handleDeactivate}
          onUpdate={handleUpdate}
          onDelete={handleDelete}
        />
      )}
    </>
  );
}

export default ActiveUsersPage;
