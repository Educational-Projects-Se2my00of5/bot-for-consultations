const API_URL = process.env.REACT_APP_API_URL || 'http://localhost:8080';

const getAuthHeaders = () => {
  const token = localStorage.getItem('adminToken');
  return {
    'Content-Type': 'application/json',
    'Authorization': `Bearer ${token}`,
  };
};

// Вход в систему
export const login = async (loginData) => {
  const response = await fetch(`${API_URL}/api/admin/login`, {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    body: JSON.stringify(loginData),
  });
  
  if (!response.ok) {
    throw new Error('Неверный логин или пароль');
  }
  
  return response.text();
};

// Проверить токен
export const checkToken = async (token) => {
  const response = await fetch(`${API_URL}/api/admin/check-token`, {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    body: JSON.stringify({ token }),
  });
  
  return response.ok;
};

// Получить всех неактивных пользователей
export const getInactiveUsers = async () => {
  const response = await fetch(`${API_URL}/api/admin/users/inactive`, {
    headers: getAuthHeaders(),
  });
  
  if (!response.ok) {
    throw new Error('Ошибка при получении неактивных пользователей');
  }
  
  return response.json();
};

// Получить всех активных пользователей
export const getActiveUsers = async () => {
  const response = await fetch(`${API_URL}/api/admin/users/active`, {
    headers: getAuthHeaders(),
  });
  
  if (!response.ok) {
    throw new Error('Ошибка при получении активных пользователей');
  }
  
  return response.json();
};

// Получить информацию о пользователе
export const getUserInfo = async (id) => {
  const response = await fetch(`${API_URL}/api/admin/users/${id}`, {
    headers: getAuthHeaders(),
  });
  
  if (!response.ok) {
    throw new Error('Ошибка при получении информации о пользователе');
  }
  
  return response.json();
};

// Активировать пользователя
export const activateUser = async (id) => {
  const response = await fetch(`${API_URL}/api/admin/users/${id}/activate`, {
    method: 'PUT',
    headers: getAuthHeaders(),
  });
  
  if (!response.ok) {
    const error = await response.text();
    throw new Error(error || 'Ошибка при активации пользователя');
  }
};

// Деактивировать пользователя
export const deactivateUser = async (id) => {
  const response = await fetch(`${API_URL}/api/admin/users/${id}/deactivate`, {
    method: 'PUT',
    headers: getAuthHeaders(),
  });
  
  if (!response.ok) {
    const error = await response.text();
    throw new Error(error || 'Ошибка при деактивации пользователя');
  }
};

// Обновить данные пользователя
export const updateUser = async (id, data) => {
  const response = await fetch(`${API_URL}/api/admin/users/${id}`, {
    method: 'PUT',
    headers: getAuthHeaders(),
    body: JSON.stringify(data),
  });
  
  if (!response.ok) {
    throw new Error('Ошибка при обновлении пользователя');
  }
};

// Удалить пользователя
export const deleteUser = async (id) => {
  const response = await fetch(`${API_URL}/api/admin/users/${id}`, {
    method: 'DELETE',
    headers: getAuthHeaders(),
  });
  
  if (!response.ok) {
    const error = await response.text();
    throw new Error(error || 'Ошибка при удалении пользователя');
  }
};
