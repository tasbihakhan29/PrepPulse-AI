# PrepPulse-AI

An AI-powered exam preparation platform that generates personalized practice tests, evaluates handwritten answers, and provides comprehensive analytics to help students master their subjects.

---

## Overview

PrepPulse-AI is a full-stack intelligent learning platform designed to revolutionize exam preparation. By leveraging cutting-edge AI models (Groq and Google Gemini), the platform transforms study materials into interactive practice tests, provides instant AI-powered evaluation of handwritten answers, and delivers detailed performance analytics. Built for students preparing for competitive exams, it offers a personalized learning experience that adapts to individual strengths and weaknesses.

### Why It Was Built

Traditional exam preparation methods are time-consuming and lack personalized feedback. PrepPulse-AI addresses this by:
- Automating test generation from study materials
- Providing instant, detailed feedback on written answers
- Tracking progress with intelligent analytics
- Offering adaptive learning recommendations

### Target Audience

- Students preparing for competitive exams (JEE, NEET, GATE, etc.)
- Educational institutions seeking modern assessment tools
- Self-learners looking for structured practice with AI feedback

---

## Features

### Core Features

- **AI-Powered Test Generation**: Upload PDFs or text materials to generate practice tests with MCQs, MSQs, and numerical questions
- **Live Test Taking**: Professional exam interface with timer, auto-save, tab switch detection, and instant evaluation
- **Answer Evaluator**: Upload handwritten answer images for AI-powered evaluation with detailed feedback
- **Smart Analytics**: Comprehensive dashboard with performance charts, topic-wise analysis, and learning insights
- **Flashcard System**: Generate and review flashcards from study materials
- **History & Progress**: Track all test attempts with detailed breakdowns and improvement trends

### Authentication & Security

- Email/password authentication with JWT tokens
- Google OAuth integration
- OTP-based password reset via email
- Secure password hashing
- Protected API routes


---

## Tech Stack

### Frontend

- **React 18** - UI library
- **Vite** - Build tool and dev server
- **TailwindCSS** - Utility-first CSS framework
- **React Router DOM** - Client-side routing
- **Lucide React** - Icon library
- **Recharts** - Data visualization charts
- **React Hook Form** - Form management
- **Axios** - HTTP client
- **@react-oauth/google** - Google OAuth integration
- **React Hot Toast** - Notification system

### Backend

- **Spring Boot 3.5.14** - Java application framework
- **Spring Data JPA** - ORM and database abstraction
- **Spring Security** - Authentication and authorization
- **PostgreSQL** - Relational database
- **Flyway** - Database migration tool
- **JWT (jjwt)** - Token-based authentication
- **Spring Mail** - Email service for OTP

### AI & External Services

- **Groq AI API (Llama 3.3 70B)** - Question generation and test creation via REST API
- **Google Gemini 2.5 Flash API** - Vision-based answer evaluation via REST API
- **Supabase Storage** - Cloud file storage for answer submissions
- **Google API Client** - OAuth token verification
- **Apache PDFBox** - PDF text extraction

### Development Tools

- **Maven** - Dependency management and build tool
- **Lombok** - Java code generation
- **PostCSS + Autoprefixer** - CSS processing

---

## Architecture

PrepPulse-AI follows a modern monolithic architecture with clear separation of concerns:

```
┌─────────────────┐
│   React Frontend │
│  (Vite + Tailwind)│
└────────┬────────┘
         │ HTTP/REST
         │ JWT Auth
┌────────▼────────┐
│  Spring Boot    │
│   Backend API   │
└────────┬────────┘
         │
    ┌────┴────┬────────────┬────────────┐
    │         │            │            │
┌───▼───┐ ┌──▼───┐   ┌────▼────┐  ┌────▼────┐
│PostgreSQL│ │Groq AI│  │Gemini AI│  │Supabase │
│Database │ │Service│  │Service  │  │Storage  │
└────────┘ └──────┘   └─────────┘  └─────────┘
```

### Data Flow

1. **Test Generation**: User uploads material → Backend extracts text → Groq AI generates questions → Stored in PostgreSQL
2. **Test Taking**: Frontend requests test → Backend loads questions → User answers → Auto-save to database → Submit → Instant evaluation
3. **Answer Evaluation**: User uploads image → Stored in Supabase → Gemini AI analyzes → Detailed feedback returned
4. **Analytics**: Backend aggregates test data → Calculates metrics → Frontend visualizes with Recharts

---

## Project Structure

```
PrepPulse-AI/
├── backend/
│   ├── src/
│   │   └── main/
│   │       ├── java/com/preppulse_ai/backend/
│   │       │   ├── config/          # Security & CORS configuration
│   │       │   ├── controller/      # REST API endpoints
│   │       │   ├── dto/             # Data transfer objects
│   │       │   ├── entity/          # JPA entities
│   │       │   ├── enums/           # Enumerations
│   │       │   ├── exception/       # Custom exceptions
│   │       │   ├── repository/      # JPA repositories
│   │       │   ├── security/        # JWT & auth logic
│   │       │   └── service/         # Business logic
│   │       └── resources/
│   │           ├── application.properties
│   │           └── db/migration/    # Flyway SQL migrations
│   ├── pom.xml                      # Maven dependencies
│   └── .env.example                 # Environment variables template
├── frontend/
│   ├── src/
│   │   ├── api/                     # API service modules
│   │   ├── assets/                  # Static assets
│   │   ├── components/              # Reusable UI components
│   │   ├── context/                 # React context providers
│   │   ├── pages/                   # Page components
│   │   ├── services/                # Utility services
│   │   ├── utils/                   # Helper functions
│   │   ├── App.jsx                  # Main app component
│   │   ├── main.jsx                 # Entry point
│   │   └── index.css                # Global styles
│   ├── package.json                 # NPM dependencies
│   ├── vite.config.js               # Vite configuration
│   ├── tailwind.config.js           # Tailwind configuration
│   └── .env.example                 # Environment variables template
├── .gitignore                       # Root gitignore
├── ANSWER_EVALUATOR_SETUP.md        # Answer evaluator documentation
├── LIVE_TEST_SETUP.md               # Live test documentation
└── README.md                        # This file
```

---



## Installation

### Prerequisites

- **Java 17** or higher
- **Node.js 18** or higher
- **PostgreSQL 14** or higher
- **Maven 3.6** or higher (or use included wrapper)
- **npm** or **yarn**

### 1. Clone the Repository

```bash
git clone https://github.com/yourusername/PrepPulse-AI.git
cd PrepPulse-AI
```

### 2. Database Setup

Create a PostgreSQL database:

```sql
CREATE DATABASE preppulse_ai;
```

### 3. Backend Setup

```bash
cd backend

# Copy environment template
cp .env.example .env

# Edit .env with your actual credentials
# IMPORTANT: Never commit .env files to Git!
# (see Environment Variables section below)

# Run with Maven wrapper (recommended)
./mvnw spring-boot:run

# Or on Windows
mvnw.cmd spring-boot:run

# Or with system Maven
mvn spring-boot:run
```

The backend will start on `http://localhost:8080`

### 4. Frontend Setup

```bash
cd frontend

# Install dependencies
npm install

# Copy environment template
cp .env.example .env

# Edit .env with your actual credentials
# IMPORTANT: Never commit .env files to Git!
# (see Environment Variables section below)

# Start development server
npm run dev
```

The frontend will start on `http://localhost:5173`

---

## Environment Variables

### Important Security Note

**Never commit `.env` files to Git!** The `.gitignore` is configured to ignore all `.env` files except `.env.example`. Always copy `.env.example` to `.env` and add your own credentials locally.

### Backend (.env)

| Variable | Description | Required | Default |
|----------|-------------|----------|---------|
| `DATABASE_URL` | PostgreSQL JDBC URL | Yes | - |
| `DATABASE_USERNAME` | Database username | Yes | - |
| `DATABASE_PASSWORD` | Database password | Yes | - |
| `JWT_SECRET` | Secret key for JWT tokens (min 32 chars) | Yes | - |
| `GOOGLE_CLIENT_ID` | Google OAuth client ID | Yes | - |
| `MAIL_HOST` | SMTP host for OTP emails | Yes | smtp.gmail.com |
| `MAIL_PORT` | SMTP port | Yes | 587 |
| `MAIL_USERNAME` | Email username | Yes | - |
| `MAIL_PASSWORD` | Email app password | Yes | - |
| `MAIL_FROM` | From address for emails | Yes | - |
| `CORS_ALLOWED_ORIGINS` | Comma-separated allowed origins | Yes | http://localhost:5173 |
| `GROQ_API_KEY` | Groq AI API key for test generation | Yes | - |
| `GROQ_MODEL` | Groq model name | No | llama-3.3-70b-versatile |
| `GEMINI_API_KEY` | Google Gemini API key for answer evaluation | Yes | - |
| `GEMINI_MODEL` | Gemini model name | No | gemini-2.5-flash |
| `SUPABASE_URL` | Supabase project URL | Yes | - |
| `SUPABASE_SERVICE_ROLE_KEY` | Supabase service role key | Yes | - |
| `SUPABASE_BUCKET_NAME` | Supabase bucket name | No | answer-submissions |

### Frontend (.env)

| Variable | Description | Required | Default |
|----------|-------------|----------|---------|
| `VITE_API_BASE_URL` | Backend API base URL (no trailing slash) | Yes | http://localhost:8080 |
| `VITE_GOOGLE_CLIENT_ID` | Google OAuth client ID | Yes | - |

---

## Usage

### 1. Sign Up / Login

- Navigate to `http://localhost:5173`
- Sign up with email/password or use Google OAuth
- Complete onboarding to set preferences

### 2. Generate Practice Tests

- Go to **Practice Center**
- Upload study materials (PDF or paste text)
- Configure test settings (exam type, difficulty, question count)
- Click **Generate Test** - AI creates questions automatically
- Save generated tests for later use

### 3. Take Live Tests

- Click **Start Test** on any generated test
- Professional exam interface with timer
- Navigate questions, mark for review
- Answers auto-save every second
- Submit when ready or wait for timer

### 4. View Results & Analytics

- Instant evaluation after submission
- View score, accuracy, time taken
- Review each question with explanations
- Check topic-wise performance charts
- Identify weak topics for improvement

### 5. Evaluate Handwritten Answers

- Go to **Answer Evaluator**
- Enter question and marks
- Upload answer image (JPG, PNG, PDF)
- AI evaluates conceptual accuracy, structure, presentation
- Get detailed feedback with strengths and weaknesses

### 6. Track Progress

- Visit **Dashboard** for overview
- Check **History & Analytics** for detailed trends
- View activity heatmap and learning insights
- Practice weak topics based on recommendations

---

## AI Features

### Test Generation (Groq AI)

**Workflow:**
1. User uploads study material (PDF or text)
2. Backend extracts text using Apache PDFBox
3. Content is sent to Groq AI (Llama 3.3 70B)
4. AI generates questions based on exam type and difficulty
5. Questions include options, correct answers, explanations, and topics
6. Generated questions are stored in database

**Models Used:**
- Primary: `llama-3.3-70b-versatile` (Groq)

**Validation:**
- Input validation for file size and type
- Content hash to prevent duplicate processing
- Error handling for API failures

### Answer Evaluation (Google Gemini)

**Workflow:**
1. User uploads answer image with question and marks
2. Image is uploaded to Supabase Storage
3. File hash is generated for caching
4. Image bytes are sent to Gemini 2.5 Flash
5. AI analyzes conceptual accuracy, technical correctness, structure, presentation
6. Returns score, feedback, improvements, strengths, weaknesses
7. Results are cached to prevent duplicate evaluations

**Models Used:**
- Primary: `gemini-2.5-flash` (Google)

**Evaluation Criteria:**
- Conceptual Accuracy
- Technical Correctness
- Structure & Organization
- Presentation Quality
- Diagram Quality (if applicable)

**Scoring:**
- Supports any marks value (2, 3, 5, 7, 10, or custom)
- Provides detailed breakdown per criterion
- Suggests improvements and missing keywords

---

## API Overview

### Authentication

- `POST /api/auth/signup` - User registration
- `POST /api/auth/login` - User login
- `POST /api/auth/google` - Google OAuth
- `POST /api/auth/forgot-password` - Request OTP
- `POST /api/auth/reset-password` - Reset password with OTP

### Practice Center

- `POST /api/practice/upload` - Upload study material
- `POST /api/practice/generate` - Generate test from material
- `GET /api/practice/tests` - Get user's generated tests
- `DELETE /api/practice/material/{id}` - Delete material
- `POST /api/practice/flashcards` - Generate flashcards
- `GET /api/practice/flashcards` - Get user's flashcards

### Test Taking

- `POST /api/tests/start` - Start or resume test attempt
- `POST /api/tests/answer` - Auto-save answer
- `POST /api/tests/submit` - Submit test for evaluation
- `GET /api/tests/{testId}` - Get test details

### Results

- `GET /api/results/{attemptId}` - Get detailed results
- `GET /api/results/{attemptId}/review` - Get question review

### Answer Evaluator

- `POST /api/evaluator/upload` - Upload answer file
- `POST /api/evaluator/evaluate` - Trigger AI evaluation
- `GET /api/evaluator/history` - Get evaluation history
- `GET /api/evaluator/{id}` - Get detailed evaluation

### Dashboard & Analytics

- `GET /api/dashboard/overview` - Get dashboard stats
- `GET /api/analytics/overview` - Get analytics overview
- `GET /api/analytics/performance-chart` - Get performance chart data
- `GET /api/analytics/heatmap` - Get activity heatmap
- `GET /api/analytics/insights` - Get learning insights

---

## Security

### Authentication

- **JWT Tokens**: Stateless authentication with 256-bit secret keys
- **Token Expiration**: Configurable token lifetime
- **Refresh Mechanism**: Secure token refresh flow

### Password Security

- **BCrypt Hashing**: Passwords hashed using Spring Security BCrypt
- **Strong Password Requirements**: Enforced via validation
- **OTP-Based Reset**: Secure password reset via email OTP

### API Security

- **Protected Routes**: All endpoints except auth require JWT
- **Ownership Validation**: Users can only access their own data
- **Input Validation**: Comprehensive validation on all inputs
- **CORS Configuration**: Configurable allowed origins
- **SQL Injection Prevention**: JPA parameterized queries

### Data Protection

- **Environment Variables**: Sensitive data never committed
- **Supabase RLS**: Storage policies for file access
- **File Hash Validation**: Integrity checks for uploads

---

## Performance Optimizations

### Database

- **Indexed Queries**: Strategic indexes on foreign keys and search fields
- **Pagination**: All list endpoints support pagination
- **Connection Pooling**: HikariCP for efficient connections
- **Flyway Migrations**: Version-controlled schema updates

### Backend

- **Caching**: Re-evaluation cache prevents duplicate AI calls
- **Async Processing**: Non-blocking file operations
- **Content Hashing**: Prevents duplicate processing
- **Lazy Loading**: JPA relationships loaded on demand

### Frontend

- **Code Splitting**: React Router lazy loading
- **Debounced API Calls**: Auto-save with 1-second debounce
- **Memoization**: React.memo for expensive components
- **Optimized Re-renders**: Proper dependency arrays

### AI Services

- **Request Batching**: Efficient API usage
- **Error Handling**: Graceful degradation on AI failures
- **Fallback Mechanisms**: Cached results when available

---

## Future Improvements

- [ ] Real-time collaborative test taking
- [ ] Mobile applications (iOS/Android)
- [ ] Advanced adaptive learning algorithms
- [ ] Video lecture integration
- [ ] Community question sharing
- [ ] Spaced repetition for flashcards
- [ ] Voice-enabled answer evaluation
- [ ] Multi-language support
- [ ] Integration with learning management systems
- [ ] Advanced analytics with ML predictions
- [ ] Offline mode with PWA
- [ ] White-label solution for institutions

---

## Contributing

We welcome contributions! Please follow these guidelines:

1. **Fork the repository**
2. **Create a feature branch**: `git checkout -b feature/amazing-feature`
3. **Commit changes**: `git commit -m 'Add amazing feature'`
4. **Push to branch**: `git push origin feature/amazing-feature`
5. **Open a Pull Request**


## Acknowledgments

- Groq AI for providing fast inference for question generation
- Google for Gemini AI vision capabilities
- Supabase for generous free tier storage
- The open-source community for amazing tools and libraries
