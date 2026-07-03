import React, { useState, useEffect } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import { testTakingApi } from '../api/testTaking';
import logoImg from '../assets/logo.png';
import toast from 'react-hot-toast';
import {
  LayoutDashboard,
  BookOpen,
  Award,
  History,
  LogOut,
  Menu,
  X,
  ChevronLeft,
  ChevronRight,
  Clock,
  CheckCircle2,
  XCircle,
  Circle,
  TrendingUp,
  BarChart3,
  Target,
  AlertCircle,
  ArrowRight,
  RefreshCw,
  Eye,
  EyeOff,
} from 'lucide-react';
import {
  ResponsiveContainer,
  PieChart,
  Pie,
  Cell,
  BarChart,
  Bar,
  XAxis,
  YAxis,
  CartesianGrid,
  Tooltip,
  Legend,
} from 'recharts';

const ResultPage = () => {
  const { user, logout } = useAuth();
  const navigate = useNavigate();
  const { attemptId } = useParams();

  const [sidebarOpen, setSidebarOpen] = useState(false);
  const [activeTab, setActiveTab] = useState('Result');

  const [resultData, setResultData] = useState(null);
  const [loading, setLoading] = useState(true);
  const [expandedExplanations, setExpandedExplanations] = useState(new Set());
  const [animatedPercentage, setAnimatedPercentage] = useState(0);
  const [animatedScore, setAnimatedScore] = useState(0);

  useEffect(() => {
    const loadResult = async () => {
      try {
        setLoading(true);
        const response = await testTakingApi.getResult(attemptId);
        setResultData(response);
      } catch (err) {
        console.error('Error loading result:', err);
        toast.error('Failed to load result. Please try again.');
        navigate('/practice-center');
      } finally {
        setLoading(false);
      }
    };

    if (user) {
      loadResult();
    }
  }, [user, attemptId, navigate]);

  useEffect(() => {
    if (!resultData) {
      return;
    }

    const targetPercentage = resultData.percentage || 0;
    const targetScore = resultData.score || 0;
    const steps = 30;
    let currentStep = 0;

    setAnimatedPercentage(0);
    setAnimatedScore(0);

    const timer = setInterval(() => {
      currentStep += 1;
      const progress = currentStep / steps;
      setAnimatedPercentage(targetPercentage * progress);
      setAnimatedScore(targetScore * progress);

      if (currentStep >= steps) {
        clearInterval(timer);
        setAnimatedPercentage(targetPercentage);
        setAnimatedScore(targetScore);
      }
    }, 25);

    return () => clearInterval(timer);
  }, [resultData]);

  const handleLogout = () => {
    logout();
    navigate('/login');
  };

  const formatTime = (seconds) => {
    const hours = Math.floor(seconds / 3600);
    const minutes = Math.floor((seconds % 3600) / 60);
    const secs = seconds % 60;
    
    if (hours > 0) {
      return `${hours}h ${minutes}m ${secs}s`;
    } else if (minutes > 0) {
      return `${minutes}m ${secs}s`;
    }
    return `${secs}s`;
  };

  const toggleExplanation = (questionId) => {
    setExpandedExplanations((prev) => {
      const newSet = new Set(prev);
      if (newSet.has(questionId)) {
        newSet.delete(questionId);
      } else {
        newSet.add(questionId);
      }
      return newSet;
    });
  };

  const getRankBadge = (percentage) => {
    if (percentage >= 90) {
      return { label: 'Future Ready', className: 'bg-emerald-400/20 text-emerald-100 border-emerald-300/30' };
    }

    if (percentage >= 75) {
      return { label: 'Future Ready', className: 'bg-cyan-400/20 text-cyan-100 border-cyan-300/30' };
    }

    if (percentage >= 60) {
      return { label: 'In Progress', className: 'bg-amber-400/20 text-amber-100 border-amber-300/30' };
    }

    return { label: 'Building Momentum', className: 'bg-white/15 text-white border-white/20' };
  };

  const reviewQuestions = resultData?.questionReviews || [];
  const questionTypePerformance = Object.values(
    reviewQuestions.reduce((accumulator, item) => {
      const key = item.questionType || 'Unknown';
      if (!accumulator[key]) {
        accumulator[key] = { questionType: key, correct: 0, total: 0 };
      }

      accumulator[key].total += 1;
      if (item.status === 'CORRECT') {
        accumulator[key].correct += 1;
      }

      return accumulator;
    }, {})
  ).map((entry) => ({
    questionType: entry.questionType,
    score: entry.total > 0 ? (entry.correct / entry.total) * 100 : 0,
  }));

  const difficultyPerformance = Object.values(
    reviewQuestions.reduce((accumulator, item) => {
      const key = item.difficulty || 'Mixed';
      if (!accumulator[key]) {
        accumulator[key] = { difficulty: key, correct: 0, total: 0 };
      }

      accumulator[key].total += 1;
      if (item.status === 'CORRECT') {
        accumulator[key].correct += 1;
      }

      return accumulator;
    }, {})
  ).map((entry) => ({
    difficulty: entry.difficulty,
    score: entry.total > 0 ? (entry.correct / entry.total) * 100 : 0,
  }));

  const marksDistribution = [
    { name: 'Correct', marks: reviewQuestions.filter((item) => item.status === 'CORRECT').reduce((sum, item) => sum + (item.marksObtained || 0), 0), color: '#10B981' },
    { name: 'Wrong', marks: reviewQuestions.filter((item) => item.status === 'WRONG').reduce((sum, item) => sum + (item.marksObtained || 0), 0), color: '#EF4444' },
    { name: 'Skipped', marks: reviewQuestions.filter((item) => item.status === 'SKIPPED').reduce((sum, item) => sum + (item.marksObtained || 0), 0), color: '#F59E0B' },
  ];

  const navigationItems = [
    { name: 'Dashboard', icon: LayoutDashboard },
    { name: 'AI Practice Center', icon: BookOpen },
    { name: 'AI Answer Evaluator', icon: Award },
    { name: 'History & Analytics', icon: History },
  ];

  const handleNavigationClick = (itemName) => {
    if (itemName === 'Dashboard') {
      navigate('/dashboard');
    } else if (itemName === 'AI Practice Center') {
      navigate('/practice-center');
    } else if (itemName === 'AI Answer Evaluator') {
      navigate('/answer-evaluator');
    } else if (itemName === 'History & Analytics') {
      navigate('/history-analytics');
    } else {
      setActiveTab(itemName);
      setSidebarOpen(false);
    }
  };

  const getWeakTopics = () => {
    if (!resultData?.topicScores) return [];
    return Object.entries(resultData.topicScores)
      .filter(([_, score]) => score < 60)
      .sort((a, b) => a[1] - b[1])
      .map(([topic, score]) => ({ topic, score }));
  };

  const COLORS = ['#10B981', '#EF4444', '#F59E0B', '#6366F1'];

  const pieData = [
    { name: 'Correct', value: resultData?.correct || 0, color: '#10B981' },
    { name: 'Wrong', value: resultData?.wrong || 0, color: '#EF4444' },
    { name: 'Skipped', value: resultData?.skipped || 0, color: '#F59E0B' },
  ];

  const topicData = Object.entries(resultData?.topicScores || {}).map(([topic, score]) => ({
    topic,
    score,
  }));

  if (loading) {
    return (
      <div className="min-h-screen bg-[#F9FAFB] flex items-center justify-center">
        <div className="animate-spin rounded-full h-12 w-12 border-t-2 border-b-2 border-[#4F46E5]"></div>
      </div>
    );
  }

  const weakTopics = getWeakTopics();

  return (
    <div className="min-h-screen bg-[#F9FAFB] flex font-sans antialiased">
      
      {/* DESKTOP SIDEBAR */}
      <aside className="hidden md:flex flex-col w-64 bg-white border-r border-gray-200 h-screen fixed left-0 top-0 z-30">
        <div className="flex items-center gap-3 px-6 h-16 border-b border-gray-100 flex-shrink-0">
          <img src={logoImg} alt="PrepPulse AI Logo" className="w-8 h-8 object-contain" />
          <span className="text-lg font-black text-gray-900 tracking-tight">PrepPulse AI</span>
        </div>
        
        <nav className="flex-1 px-4 py-6 space-y-1 overflow-y-auto">
          {navigationItems.map((item) => {
            const Icon = item.icon;
            const isActive = activeTab === item.name;
            return (
              <button
                key={item.name}
                onClick={() => handleNavigationClick(item.name)}
                className={`w-full flex items-center gap-3 px-4 py-3 rounded-xl text-sm font-bold transition-all duration-150 ${
                  isActive
                    ? 'bg-indigo-50 text-[#4F46E5]'
                    : 'text-gray-500 hover:bg-gray-50 hover:text-gray-900'
                }`}
              >
                <Icon className={`w-5 h-5 ${isActive ? 'text-[#4F46E5]' : 'text-gray-400'}`} />
                <span>{item.name}</span>
              </button>
            );
          })}
        </nav>

        <div className="p-4 border-t border-gray-100">
          <button
            onClick={handleLogout}
            className="w-full flex items-center gap-3 px-4 py-3 rounded-xl text-sm font-bold text-gray-500 hover:bg-red-50 hover:text-[#EF4444] transition-all duration-150"
          >
            <LogOut className="w-5 h-5 text-gray-400" />
            <span>Logout</span>
          </button>
        </div>
      </aside>

      {/* MOBILE SIDEBAR */}
      {sidebarOpen && (
        <div
          className="md:hidden fixed inset-0 bg-gray-900/40 backdrop-blur-sm z-40 transition-opacity"
          onClick={() => setSidebarOpen(false)}
        />
      )}
      
      <div
        className={`md:hidden fixed top-0 bottom-0 left-0 w-64 bg-white border-r border-gray-200 z-50 p-6 flex flex-col justify-between transition-transform duration-300 ease-in-out ${
          sidebarOpen ? 'translate-x-0' : '-translate-x-full'
        }`}
      >
        <div className="space-y-6">
          <div className="flex items-center justify-between border-b border-gray-100 pb-4">
            <div className="flex items-center gap-3">
              <img src={logoImg} alt="PrepPulse AI Logo" className="w-7 h-7 object-contain" />
              <span className="text-base font-black text-gray-900 tracking-tight">PrepPulse AI</span>
            </div>
            <button
              onClick={() => setSidebarOpen(false)}
              className="p-1.5 hover:bg-gray-100 rounded-lg text-gray-500"
            >
              <X className="w-5 h-5" />
            </button>
          </div>

          <nav className="space-y-1">
            {navigationItems.map((item) => {
              const Icon = item.icon;
              const isActive = activeTab === item.name;
              return (
                <button
                  key={item.name}
                  onClick={() => handleNavigationClick(item.name)}
                  className={`w-full flex items-center gap-3 px-4 py-3 rounded-xl text-sm font-bold transition-all duration-150 ${
                    isActive
                      ? 'bg-indigo-50 text-[#4F46E5]'
                      : 'text-gray-500 hover:bg-gray-50 hover:text-gray-950'
                  }`}
                >
                  <Icon className={`w-5 h-5 ${isActive ? 'text-[#4F46E5]' : 'text-gray-400'}`} />
                  <span>{item.name}</span>
                </button>
              );
            })}
          </nav>
        </div>

        <div className="border-t border-gray-100 pt-4">
          <button
            onClick={handleLogout}
            className="w-full flex items-center gap-3 px-4 py-3 rounded-xl text-sm font-bold text-gray-500 hover:bg-red-50 hover:text-[#EF4444] transition-all duration-150"
          >
            <LogOut className="w-5 h-5 text-gray-400" />
            <span>Logout</span>
          </button>
        </div>
      </div>

      {/* MAIN CONTENT */}
      <div className="md:pl-64 flex flex-col flex-1 min-h-screen overflow-x-hidden">
        
        {/* TOP BAR */}
        <header className="sticky top-0 bg-white/80 backdrop-blur-md border-b border-gray-200/80 h-16 flex items-center justify-between px-6 z-20">
          <div className="flex items-center gap-3">
            <button
              className="md:hidden p-2 text-gray-500 hover:bg-gray-100 rounded-xl transition-colors"
              onClick={() => setSidebarOpen(true)}
            >
              <Menu className="w-5 h-5" />
            </button>
            <span className="font-extrabold text-gray-900 text-lg hidden md:inline">
              {activeTab}
            </span>
          </div>

          <div className="flex items-center gap-2">
            {user.profilePicture ? (
              <img
                src={user.profilePicture}
                alt={user.name}
                className="w-9 h-9 rounded-full border-2 border-indigo-100 object-cover shadow-sm"
                referrerPolicy="no-referrer"
              />
            ) : (
              <div className="w-9 h-9 rounded-full bg-[#4F46E5] flex items-center justify-center text-white font-extrabold text-xs shadow-sm">
                {user.name.charAt(0).toUpperCase()}
              </div>
            )}
          </div>
        </header>

        {/* RESULT CONTENT */}
        <main className="flex-1 p-6 md:p-8 bg-[#F9FAFB] overflow-y-auto">
          <div className="max-w-6xl mx-auto space-y-8">
            
            {/* SCORE OVERVIEW */}
            <div className="bg-gradient-to-r from-indigo-500 via-indigo-600 to-indigo-700 text-white rounded-3xl p-8 shadow-xl shadow-indigo-100/50">
              <div className="grid grid-cols-1 md:grid-cols-3 gap-8 items-center">
                
                {/* Circular Progress */}
                <div className="flex justify-center">
                  <div className="relative">
                    <svg width="200" height="200" viewBox="0 0 200 200">
                      <circle
                        cx="100"
                        cy="100"
                        r="90"
                        fill="none"
                        stroke="rgba(255,255,255,0.2)"
                        strokeWidth="12"
                      />
                      <circle
                        cx="100"
                        cy="100"
                        r="90"
                        fill="none"
                        stroke="#10B981"
                        strokeWidth="12"
                        strokeLinecap="round"
                        strokeDasharray={`${2 * Math.PI * 90}`}
                        strokeDashoffset={`${2 * Math.PI * 90 * (1 - animatedPercentage / 100)}`}
                        transform="rotate(-90 100 100)"
                      />
                    </svg>
                    <div className="absolute inset-0 flex flex-col items-center justify-center">
                      <span className="text-4xl font-black">
                        {animatedPercentage.toFixed(1)}%
                      </span>
                      <span className="text-sm font-semibold text-indigo-100">Score</span>
                    </div>
                  </div>
                </div>

                {/* Score Details */}
                <div className="space-y-4">
                  <div className="inline-flex items-center gap-2 rounded-full border px-3 py-1 text-xs font-black uppercase tracking-[0.18em]">
                    {getRankBadge(resultData?.percentage || 0).label}
                  </div>
                  <div>
                    <p className="text-indigo-100 text-sm font-semibold mb-1">Overall Score</p>
                    <p className="text-3xl font-black">{animatedScore.toFixed(2)}</p>
                  </div>
                  <div>
                    <p className="text-indigo-100 text-sm font-semibold mb-1">Time Taken</p>
                    <p className="text-xl font-bold flex items-center gap-2">
                      <Clock className="w-5 h-5" />
                      {formatTime(resultData?.timeTaken || 0)}
                    </p>
                  </div>
                  <div>
                    <p className="text-indigo-100 text-sm font-semibold mb-1">Accuracy</p>
                    <p className="text-xl font-bold">
                      {resultData?.attempted > 0 
                        ? ((resultData.correct / resultData.attempted) * 100).toFixed(1) 
                        : 0}%
                    </p>
                  </div>
                </div>

                {/* Stats */}
                <div className="grid grid-cols-2 gap-4">
                  <div className="bg-white/10 rounded-xl p-4 text-center">
                    <p className="text-3xl font-black">{resultData?.attempted || 0}</p>
                    <p className="text-xs font-semibold text-indigo-100">Attempted</p>
                  </div>
                  <div className="bg-white/10 rounded-xl p-4 text-center">
                    <p className="text-3xl font-black text-emerald-300">{resultData?.correct || 0}</p>
                    <p className="text-xs font-semibold text-indigo-100">Correct</p>
                  </div>
                  <div className="bg-white/10 rounded-xl p-4 text-center">
                    <p className="text-3xl font-black text-red-300">{resultData?.wrong || 0}</p>
                    <p className="text-xs font-semibold text-indigo-100">Wrong</p>
                  </div>
                  <div className="bg-white/10 rounded-xl p-4 text-center">
                    <p className="text-3xl font-black text-yellow-300">{resultData?.skipped || 0}</p>
                    <p className="text-xs font-semibold text-indigo-100">Skipped</p>
                  </div>
                </div>
              </div>
            </div>

            {/* PERFORMANCE CHARTS */}
            <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
              <div className="bg-white rounded-2xl border border-gray-200/80 shadow-sm p-6">
                <div className="flex items-center justify-between mb-6">
                  <h3 className="text-base font-extrabold text-gray-900">Marks Distribution</h3>
                  <div className="p-2 bg-indigo-50 border border-indigo-100/30 rounded-lg text-[#4F46E5]">
                    <PieChart className="w-4 h-4" />
                  </div>
                </div>
                <ResponsiveContainer width="100%" height={250}>
                  <BarChart data={marksDistribution}>
                    <CartesianGrid strokeDasharray="3 3" stroke="#F1F5F9" />
                    <XAxis dataKey="name" stroke="#94A3B8" fontSize={10} fontWeight={700} tickLine={false} axisLine={false} />
                    <YAxis stroke="#94A3B8" fontSize={10} fontWeight={700} tickLine={false} axisLine={false} />
                    <Tooltip />
                    <Bar dataKey="marks" radius={[4, 4, 0, 0]}>
                      {marksDistribution.map((entry, index) => (
                        <Cell key={`marks-cell-${index}`} fill={entry.color} />
                      ))}
                    </Bar>
                  </BarChart>
                </ResponsiveContainer>
              </div>

              <div className="bg-white rounded-2xl border border-gray-200/80 shadow-sm p-6">
                <div className="flex items-center justify-between mb-6">
                  <h3 className="text-base font-extrabold text-gray-900">Topic-wise Performance</h3>
                  <div className="p-2 bg-indigo-50 border border-indigo-100/30 rounded-lg text-[#4F46E5]">
                    <BarChart3 className="w-4 h-4" />
                  </div>
                </div>
                <ResponsiveContainer width="100%" height={250}>
                  <BarChart data={topicData}>
                    <CartesianGrid strokeDasharray="3 3" stroke="#F1F5F9" />
                    <XAxis dataKey="topic" stroke="#94A3B8" fontSize={10} fontWeight={700} tickLine={false} axisLine={false} />
                    <YAxis stroke="#94A3B8" fontSize={10} fontWeight={700} tickLine={false} axisLine={false} domain={[0, 100]} />
                    <Tooltip />
                    <Bar dataKey="score" fill="#4F46E5" radius={[4, 4, 0, 0]} />
                  </BarChart>
                </ResponsiveContainer>
              </div>
            </div>

            <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
              <div className="bg-white rounded-2xl border border-gray-200/80 shadow-sm p-6">
                <div className="flex items-center justify-between mb-6">
                  <h3 className="text-base font-extrabold text-gray-900">Difficulty-wise Performance</h3>
                  <div className="p-2 bg-indigo-50 border border-indigo-100/30 rounded-lg text-[#4F46E5]">
                    <TrendingUp className="w-4 h-4" />
                  </div>
                </div>
                <ResponsiveContainer width="100%" height={250}>
                  <BarChart data={difficultyPerformance}>
                    <CartesianGrid strokeDasharray="3 3" stroke="#F1F5F9" />
                    <XAxis dataKey="difficulty" stroke="#94A3B8" fontSize={10} fontWeight={700} tickLine={false} axisLine={false} />
                    <YAxis stroke="#94A3B8" fontSize={10} fontWeight={700} tickLine={false} axisLine={false} domain={[0, 100]} />
                    <Tooltip />
                    <Bar dataKey="score" fill="#10B981" radius={[4, 4, 0, 0]} />
                  </BarChart>
                </ResponsiveContainer>
              </div>

              <div className="bg-white rounded-2xl border border-gray-200/80 shadow-sm p-6">
                <div className="flex items-center justify-between mb-6">
                  <h3 className="text-base font-extrabold text-gray-900">Question Type Performance</h3>
                  <div className="p-2 bg-indigo-50 border border-indigo-100/30 rounded-lg text-[#4F46E5]">
                    <BarChart3 className="w-4 h-4" />
                  </div>
                </div>
                <ResponsiveContainer width="100%" height={250}>
                  <BarChart data={questionTypePerformance}>
                    <CartesianGrid strokeDasharray="3 3" stroke="#F1F5F9" />
                    <XAxis dataKey="questionType" stroke="#94A3B8" fontSize={10} fontWeight={700} tickLine={false} axisLine={false} />
                    <YAxis stroke="#94A3B8" fontSize={10} fontWeight={700} tickLine={false} axisLine={false} domain={[0, 100]} />
                    <Tooltip />
                    <Bar dataKey="score" fill="#4F46E5" radius={[4, 4, 0, 0]} />
                  </BarChart>
                </ResponsiveContainer>
              </div>
            </div>

            {/* WEAK TOPICS */}
            {weakTopics.length > 0 && (
              <div className="bg-white rounded-2xl border border-gray-200/80 shadow-sm p-6">
                <div className="flex items-center justify-between mb-6">
                  <h3 className="text-base font-extrabold text-gray-900">Weak Topics</h3>
                  <div className="p-2 bg-orange-50 border border-orange-100/30 rounded-lg text-orange-600">
                    <AlertCircle className="w-4 h-4" />
                  </div>
                </div>
                <div className="space-y-4">
                  {weakTopics.map((item, index) => (
                    <div key={index} className="flex items-center justify-between p-4 bg-orange-50 rounded-xl">
                      <div className="flex items-center gap-3">
                        <Target className="w-5 h-5 text-orange-600" />
                        <span className="text-sm font-bold text-gray-800">{item.topic}</span>
                      </div>
                      <div className="flex items-center gap-3">
                        <span className="text-sm font-black text-orange-600">{item.score.toFixed(1)}%</span>
                        <button
                          onClick={() => navigate('/practice-center', { state: { presetFilters: { topicFocus: item.topic } } })}
                          className="flex items-center gap-2 px-3 py-1.5 bg-orange-600 text-white text-xs font-bold rounded-lg hover:bg-orange-700 transition-all"
                        >
                          Practice Again
                          <ArrowRight className="w-3 h-3" />
                        </button>
                        <button
                          onClick={() => navigate('/practice-center', { state: { presetFilters: { topicFocus: item.topic, questionType: 'Mixed', difficulty: 'Mixed' } } })}
                          className="flex items-center gap-2 px-3 py-1.5 bg-white text-orange-700 border border-orange-200 text-xs font-bold rounded-lg hover:bg-orange-50 transition-all"
                        >
                          Generate Similar Questions
                          <ArrowRight className="w-3 h-3" />
                        </button>
                      </div>
                    </div>
                  ))}
                </div>
              </div>
            )}

            {/* QUESTION REVIEW */}
            <div className="bg-white rounded-2xl border border-gray-200/80 shadow-sm overflow-hidden">
              <div className="p-6 border-b border-gray-100">
                <h3 className="text-base font-extrabold text-gray-900">Question Review</h3>
                <p className="textxs text-gray-400 font-semibold mt-1">Review each question with correct answers and explanations</p>
              </div>

              <div className="divide-y divide-gray-100">
                {resultData?.questionReviews?.map((question, index) => {
                  const statusIcon = {
                    'CORRECT': <CheckCircle2 className="w-5 h-5 text-emerald-500" />,
                    'WRONG': <XCircle className="w-5 h-5 text-red-500" />,
                    'SKIPPED': <Circle className="w-5 h-5 text-gray-400" />,
                  }[question.status];

                  const statusColor = {
                    'CORRECT': 'bg-emerald-50 text-emerald-700 border-emerald-200',
                    'WRONG': 'bg-red-50 text-red-700 border-red-200',
                    'SKIPPED': 'bg-gray-50 text-gray-700 border-gray-200',
                  }[question.status];

                  return (
                    <div key={question.questionId} className="p-6 space-y-4">
                      
                      {/* Question Header */}
                      <div className="flex items-start justify-between gap-4">
                        <div className="flex items-center gap-3">
                          <span className="px-3 py-1 bg-[#4F46E5] text-white text-sm font-black rounded-lg">
                            Q{question.questionNumber}
                          </span>
                          {question.questionType && (
                            <span className="px-3 py-1 bg-indigo-50 text-[#4F46E5] text-xs font-bold rounded-lg">
                              {question.questionType}
                            </span>
                          )}
                          <div className={`flex items-center gap-2 px-3 py-1 rounded-lg text-xs font-bold border ${statusColor}`}>
                            {statusIcon}
                            {question.status}
                          </div>
                          {question.marksObtained !== null && (
                            <span className="text-sm font-bold text-gray-600">
                              {question.marksObtained > 0 ? '+' : ''}{question.marksObtained}
                            </span>
                          )}
                        </div>
                        {question.topic && (
                          <span className="px-3 py-1 bg-gray-100 text-gray-700 text-xs font-bold rounded-lg">
                            {question.topic}
                          </span>
                        )}
                      </div>

                      {/* Question */}
                      <p className="text-sm font-semibold text-gray-800 leading-relaxed">
                        {question.question}
                      </p>

                      {/* Options */}
                      <div className="space-y-2">
                        {question.options?.map((option, optIndex) => {
                          const optionLetter = String.fromCharCode(65 + optIndex);
                          const isCorrect = optionLetter === question.correctAnswer;
                          const isUserAnswer = optionLetter === question.userAnswer;

                          let bgColor = 'bg-gray-50 border-gray-200';
                          if (isCorrect) bgColor = 'bg-emerald-50 border-emerald-300';
                          if (isUserAnswer && !isCorrect) bgColor = 'bg-red-50 border-red-300';

                          return (
                            <div
                              key={optIndex}
                              className={`flex items-center gap-3 p-3 rounded-lg border-2 ${bgColor}`}
                            >
                              <span className="w-6 h-6 flex items-center justify-center bg-white rounded font-bold text-gray-700 text-xs">
                                {optionLetter}
                              </span>
                              <span className="text-sm font-medium text-gray-800">{option}</span>
                              {isCorrect && <CheckCircle2 className="w-4 h-4 text-emerald-500 ml-auto" />}
                              {isUserAnswer && !isCorrect && <XCircle className="w-4 h-4 text-red-500 ml-auto" />}
                            </div>
                          );
                        })}
                      </div>

                      {/* Correct Answer */}
                      {question.status !== 'CORRECT' && (
                        <div className="bg-indigo-50 rounded-lg p-3">
                          <p className="text-xs font-black text-indigo-600 uppercase tracking-wider mb-1">Correct Answer</p>
                          <p className="text-sm font-bold text-indigo-900">{question.correctAnswer}</p>
                        </div>
                      )}

                      {/* Explanation */}
                      {question.explanation && (
                        <div className="border border-gray-200 rounded-lg overflow-hidden">
                          <button
                            onClick={() => toggleExplanation(question.questionId)}
                            className="w-full flex items-center justify-between p-3 bg-gray-50 hover:bg-gray-100 transition-all"
                          >
                            <span className="text-sm font-bold text-gray-700 flex items-center gap-2">
                              <TrendingUp className="w-4 h-4 text-[#4F46E5]" />
                              AI Explanation
                            </span>
                            {expandedExplanations.has(question.questionId) ? (
                              <EyeOff className="w-4 h-4 text-gray-500" />
                            ) : (
                              <Eye className="w-4 h-4 text-gray-500" />
                            )}
                          </button>
                          {expandedExplanations.has(question.questionId) && (
                            <div className="p-4 bg-white">
                              <p className="text-sm text-gray-700 leading-relaxed">{question.explanation}</p>
                            </div>
                          )}
                        </div>
                      )}
                    </div>
                  );
                })}
              </div>
            </div>

            {/* ACTIONS */}
            <div className="flex items-center justify-center gap-4">
              <button
                onClick={() => navigate('/practice-center')}
                className="inline-flex items-center justify-center px-6 py-3 bg-[#4F46E5] hover:bg-[#4338CA] text-white text-sm font-black rounded-2xl shadow-xl shadow-indigo-100/50 active:scale-[0.98] transition-all"
              >
                <RefreshCw className="w-4.5 h-4.5 mr-2" />
                Practice More
              </button>
              <button
                onClick={() => navigate('/dashboard')}
                className="inline-flex items-center justify-center px-6 py-3 border border-gray-200 bg-white text-gray-700 hover:bg-gray-50 text-sm font-bold rounded-2xl transition-all"
              >
                Go to Dashboard
              </button>
            </div>
          </div>
        </main>
      </div>
    </div>
  );
};

export default ResultPage;
