# PrepPulse AI

> An AI-powered exam preparation platform that transforms study material into personalized practice tests, evaluates handwritten answers, and provides actionable performance insights.

[![React](https://img.shields.io/badge/Frontend-React%2018-61DAFB?logo=react&logoColor=white)](https://react.dev/)
[![Spring Boot](https://img.shields.io/badge/Backend-Spring%20Boot-6DB33F?logo=springboot&logoColor=white)](https://spring.io/projects/spring-boot)
[![PostgreSQL](https://img.shields.io/badge/Database-PostgreSQL-4169E1?logo=postgresql&logoColor=white)](https://www.postgresql.org/)
[![Java](https://img.shields.io/badge/Java-17%2B-ED8B00?logo=openjdk&logoColor=white)](https://www.java.com/)
[![AI](https://img.shields.io/badge/AI-Groq%20%2B%20Gemini-8E75B2)](https://ai.google.dev/)

---

## 🎥 Demo

Watch the PrepPulse AI demo:

**[▶️ View Demo on Google Drive](https://drive.google.com/file/d/1BqFahEcTzD69c0QuYuz-M4vh89KF6tBN/view?usp=sharing)**

---

## 📌 Overview

Preparing for exams often involves manually creating practice questions, checking written answers, and tracking performance across different topics.

**PrepPulse AI** brings these activities into one platform.

Users can provide their study material as a PDF or text, configure their exam requirements, and generate AI-powered practice tests. After completing a test, the platform evaluates the attempt and provides detailed results and performance insights.

It also includes an **AI-powered handwritten answer evaluator** that analyzes uploaded answer images and provides structured feedback.

---

## 🎯 Problem

Students commonly face three challenges:

- Creating quality practice questions from their own study material is time-consuming.
- Written answers require manual evaluation and feedback.
- Performance data is often scattered, making it difficult to identify weak areas.

### 💡 Solution

PrepPulse AI combines **AI-powered question generation, live test-taking, handwritten answer evaluation, and performance analytics** into a single learning platform.

---

## ✨ Key Features

### 🤖 AI Practice Test Generation

Generate personalized practice tests from your own study material.

- Upload PDF study material or provide text
- Select exam context
- Choose question format
- Configure difficulty and question count
- Focus on selected topics
- Generate questions using AI

Supported question formats include:

- MCQ
- MSQ
- Numerical

---

### 📝 Live Test Taking

A dedicated test-taking experience allows students to practice in an exam-like environment.

- Countdown timer
- Question navigation
- Answer saving
- Mark questions for review
- Tab-switch detection
- Automatic evaluation after submission

---

### 🧠 AI Handwritten Answer Evaluation

Students can upload handwritten answers and receive AI-powered feedback.

The evaluator analyzes aspects such as:

- Conceptual accuracy
- Technical correctness
- Structure and organization
- Presentation quality
- Diagrams, where applicable

It provides:

- Score
- Strengths
- Weaknesses
- Detailed feedback
- Suggested improvements
- Missing concepts or keywords

---

### 📊 Dashboard & Analytics

Track learning progress through a centralized dashboard.

Analytics include:

- Test performance
- Accuracy
- Performance trends
- Topic-wise performance
- Difficulty analysis
- Question-type distribution
- Learning insights
- Recommendations for improvement

---

### 📚 History & Progress

Students can review their previous learning activity and identify performance patterns over time.

---

### 🔐 Authentication & Security

PrepPulse AI includes secure authentication and protected APIs.

- Email/password authentication
- JWT-based authentication
- Google OAuth
- OTP-based password reset
- Password hashing with BCrypt
- Protected API endpoints
- User-specific data access
- Input validation

---

# 🏗️ System Architecture

```text
                         ┌──────────────────────┐
                         │      User            │
                         └──────────┬───────────┘
                                    │
                                    ▼
                    ┌────────────────────────────┐
                    │      React Frontend        │
                    │   React + Vite + Tailwind  │
                    └──────────────┬─────────────┘
                                   │
                              REST / JWT
                                   │
                                   ▼
                    ┌────────────────────────────┐
                    │     Spring Boot Backend    │
                    │      REST API + Security   │
                    └───────┬──────────┬─────────┘
                            │          │
                 ┌──────────┘          └─────────────┐
                 ▼                                    ▼
        ┌─────────────────┐                  ┌─────────────────┐
        │   PostgreSQL    │                  │   AI Services   │
        │                 │                  │                 │
        │ Tests           │                  │ Groq            │
        │ Attempts        │                  │ Gemini          │
        │ Results         │                  │                 │
        │ Analytics       │                  └─────────────────┘
        └─────────────────┘
                                                   │
                                                   ▼
                                          ┌─────────────────┐
                                          │ Supabase Storage│
                                          │                 │
                                          │ Answer Files    │
                                          └─────────────────┘
