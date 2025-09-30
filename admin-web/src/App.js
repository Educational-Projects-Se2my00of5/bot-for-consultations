import React from 'react';
import { BrowserRouter as Router, Routes, Route, Navigate } from 'react-router-dom';
import LoginPage from './components/LoginPage';
import './App.css';

function App() {
    return (
        <Router>
            <div className="App">
                <Routes>
                    {/* По умолчанию перенаправляем на страницу входа */}
                    <Route path="/" element={<Navigate replace to="/login" />} />

                    {/* Маршрут для страницы входа */}
                    <Route path="/login" element={<LoginPage />} />

                    {/* Маршрут для страницы регистрации */}

                </Routes>
            </div>
        </Router>
    );
}

export default App;