import React, { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import { historyAnalyticsApi } from '../api/historyAnalytics';
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
  TrendingUp,
  BarChart3,
  PieChart,
  Calendar,
  Flame,
  Clock,
  Target,
  CheckCircle2,
  XCircle,
  AlertTriangle,
  Download,
  Search,
  Trash2,
  Eye,
  RefreshCw,
  ArrowRight,
} from 'lucide-react';
import {
  ResponsiveContainer,
  LineChart,
  Line,
  AreaChart,
  Area,
  BarChart,
  Bar,
  PieChart as RechartsPieChart,
  Pie,
  Cell,
  XAxis,
  YAxis,
  CartesianGrid,
  Tooltip,
  Legend,
} from 'recharts';

const HistoryAnalyticsPage = () => {
  const { user, logout } = useAuth();
  const navigate = useNavigate();

  const [sidebarOpen, setSidebarOpen] = useState(false);
  const [activeTab, setActiveTab] = useState('History & Analytics');

  // Analytics data
  const [overview, setOverview] = useState(null);
  const [performanceChart, setPerformanceChart] = useState([]);
  const [topicPerformance, setTopicPerformance] = useState([]);
  const [heatmapData, setHeatmapData] = useState([]);
  const [questionTypeDist, setQuestionTypeDist] = useState([]);
  const [difficultyAnalysis, setDifficultyAnalysis] = useState([]);
  const [insights, setInsights] = useState([]);
  const [recommendations, setRecommendations] = useState(null);

  // History data
  const [testHistory, setTestHistory] = useState({ content: [], totalElements: 0 });
  const [testHistoryError, setTestHistoryError] = useState(null);
  const [evaluationHistory, setEvaluationHistory] = useState({ content: [], totalElements: 0 });

  // UI state
  const [loading, setLoading] = useState(true);
  const [activeSection, setActiveSection] = useState('overview');
  const [searchQuery, setSearchQuery] = useState('');
  const [testPage, setTestPage] = useState(0);
  const [evalPage, setEvalPage] = useState(0);

  useEffect(() => {
    loadAnalyticsData();
  }, [user]);

  const loadAnalyticsData = async () => {
    const historyPromise = loadHistoryData();

    try {
      setLoading(true);
      const [
        overviewData,
        perfData,
        topicData,
        heatmap,
        typeDist,
        diffAnalysis,
        insightsData,
        recs,
      ] = await Promise.all([
        historyAnalyticsApi.getAnalyticsOverview(),
        historyAnalyticsApi.getPerformanceChartData(30),
        historyAnalyticsApi.getTopicPerformance(),
        historyAnalyticsApi.getHeatmapData(365),
        historyAnalyticsApi.getQuestionTypeDistribution(),
        historyAnalyticsApi.getDifficultyAnalysis(),
        historyAnalyticsApi.getLearningInsights(),
        historyAnalyticsApi.getRecommendations(),
      ]);

      setOverview(overviewData);
      setPerformanceChart(perfData);
      setTopicPerformance(topicData);
      setHeatmapData(heatmap);
      setQuestionTypeDist(typeDist);
      setDifficultyAnalysis(diffAnalysis);
      setInsights(insightsData);
      setRecommendations(recs);
    } catch (err) {
      console.error('Error loading analytics:', err);
      toast.error('Failed to load analytics data');
    } finally {
      await historyPromise;
      setLoading(false);
    }
  };

  const loadHistoryData = async () => {
    setTestHistoryError(null);

    try {
      const tests = await historyAnalyticsApi.getTestHistory(0, 10);
      setTestHistory(tests);
    } catch (err) {
      console.error('Error loading Test History from /api/history/tests:', err);
      setTestHistoryError(
        err.response?.data?.message || err.message || 'Unable to load Test History.'
      );
    }

    try {
      const evals = await historyAnalyticsApi.getEvaluationHistory('', 0, 10);

      setEvaluationHistory(evals);
    } catch (err) {
      console.error('Error loading evaluation history:', err);
    }
  };

  const handleLogout = () => {
    logout();
    navigate('/login');
  };

  const handleDeleteTest = async (attemptId) => {
    if (!confirm('Are you sure you want to delete this test history?')) return;
    try {
      await historyAnalyticsApi.deleteTestHistory(attemptId);
      toast.success('Test history deleted');
      loadHistoryData();
    } catch (err) {
      toast.error('Failed to delete test history');
    }
  };

  const handleDeleteEvaluation = async (submissionId) => {
    if (!confirm('Are you sure you want to delete this evaluation?')) return;
    try {
      await historyAnalyticsApi.deleteEvaluationHistory(submissionId);
      toast.success('Evaluation deleted');
      loadHistoryData();
    } catch (err) {
      toast.error('Failed to delete evaluation');
    }
  };

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

  const COLORS = ['#4F46E5', '#10B981', '#F59E0B', '#EF4444', '#8B5CF6', '#EC4899'];

  const getHeatmapColor = (level) => {
    const colors = ['#F3F4F6', '#C7D2FE', '#A5B4FC', '#818CF8', '#6366F1', '#4F46E5'];
    return colors[level] || '#F3F4F6';
  };

  const getStatusColor = (status) => {
    return status === 'STRONG' ? 'text-emerald-600 bg-emerald-50' :
           status === 'WEAK' ? 'text-red-600 bg-red-50' :
           'text-yellow-600 bg-yellow-50';
  };

  if (loading) {
    return (
      <div className="min-h-screen bg-[#F9FAFB] flex items-center justify-center">
        <div className="animate-spin rounded-full h-12 w-12 border-t-2 border-b-2 border-[#4F46E5]"></div>
      </div>
    );
  }

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

        {/* ANALYTICS CONTENT */}
        <main className="flex-1 p-6 md:p-8 bg-[#F9FAFB] overflow-y-auto">
          <div className="max-w-7xl mx-auto space-y-8">
            
            {/* SECTION NAVIGATION */}
            <div className="flex flex-wrap gap-2">
              {['overview', 'performance', 'topics', 'heatmap', 'tests', 'evaluations', 'insights'].map((section) => (
                <button
                  key={section}
                  onClick={() => setActiveSection(section)}
                  className={`px-4 py-2 rounded-lg text-sm font-bold transition-all ${
                    activeSection === section
                      ? 'bg-[#4F46E5] text-white'
                      : 'bg-white text-gray-600 hover:bg-gray-100'
                  }`}
                >
                  {section.charAt(0).toUpperCase() + section.slice(1)}
                </button>
              ))}
            </div>

            {/* OVERVIEW SECTION */}
            {activeSection === 'overview' && (
              <div className="space-y-6">
                {/* KPI Cards */}
                <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
                  {[
                    { label: 'Total Tests Taken', value: overview?.totalTestsTaken || 0, icon: BookOpen, color: 'indigo' },
                    { label: 'Total Evaluations', value: overview?.totalEvaluations || 0, icon: Award, color: 'emerald' },
                    { label: 'Current Streak 🔥', value: overview?.currentStudyStreak || 0, icon: Flame, color: 'red' },
                    { label: 'Average Score', value: overview?.averageScore?.toFixed(1) || 0, icon: TrendingUp, color: 'purple' },
                    { label: 'Best Score', value: overview?.bestScore?.toFixed(1) || 0, icon: CheckCircle2, color: 'green' },
                    { label: 'Hours Studied', value: overview?.hoursStudied || 0, icon: Clock, color: 'blue' },
                    { label: 'Questions Attempted', value: overview?.totalQuestionsAttempted || 0, icon: BarChart3, color: 'pink' },
                  ].map((kpi, index) => {
                    const Icon = kpi.icon;
                    const colorClasses = {
                      indigo: 'bg-indigo-50 text-indigo-600',
                      emerald: 'bg-emerald-50 text-emerald-600',
                      amber: 'bg-amber-50 text-amber-600',
                      red: 'bg-red-50 text-red-600',
                      purple: 'bg-purple-50 text-purple-600',
                      green: 'bg-green-50 text-green-600',
                      blue: 'bg-blue-50 text-blue-600',
                      pink: 'bg-pink-50 text-pink-600',
                    }[kpi.color];

                    return (
                      <div key={index} className="bg-white rounded-2xl border border-gray-200/80 p-6 shadow-sm hover:shadow-md transition-shadow">
                        <div className="flex items-center justify-between mb-4">
                          <div className={`p-3 rounded-xl ${colorClasses}`}>
                            <Icon className="w-5 h-5" />
                          </div>
                        </div>
                        <p className="text-3xl font-black text-gray-900">{kpi.value}</p>
                        <p className="text-sm font-semibold text-gray-500 mt-1">{kpi.label}</p>
                      </div>
                    );
                  })}
                </div>

                {/* Recommendations */}
                {recommendations && (
                  <div className="bg-gradient-to-r from-indigo-500 to-purple-600 text-white rounded-2xl p-6 shadow-xl">
                    <h3 className="text-lg font-black mb-4 flex items-center gap-2">
                      <Target className="w-5 h-5" />
                      Smart Recommendations
                    </h3>
                    <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-4">
                      <div className="bg-white/10 rounded-xl p-4">
                        <p className="text-xs font-semibold text-indigo-100 mb-1">Recommended Topic</p>
                        <p className="text-sm font-bold">{recommendations.recommendedTopic}</p>
                      </div>
                      <div className="bg-white/10 rounded-xl p-4">
                        <p className="text-xs font-semibold text-indigo-100 mb-1">Suggested Difficulty</p>
                        <p className="text-sm font-bold">{recommendations.suggestedDifficulty}</p>
                      </div>
                      <div className="bg-white/10 rounded-xl p-4">
                        <p className="text-xs font-semibold text-indigo-100 mb-1">Daily Goal</p>
                        <p className="text-sm font-bold">{recommendations.recommendedDailyGoal} questions</p>
                      </div>
                    </div>
                  </div>
                )}
              </div>
            )}

            {/* PERFORMANCE CHARTS SECTION */}
            {activeSection === 'performance' && (
              <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
                {/* Score Trend */}
                <div className="bg-white rounded-2xl border border-gray-200/80 shadow-sm p-6">
                  <h3 className="text-base font-extrabold text-gray-900 mb-6 flex items-center gap-2">
                    <TrendingUp className="w-5 h-5 text-[#4F46E5]" />
                    Score Trend (30 Days)
                  </h3>
                  <ResponsiveContainer width="100%" height={300}>
                    <LineChart data={performanceChart}>
                      <CartesianGrid strokeDasharray="3 3" stroke="#F1F5F9" />
                      <XAxis dataKey="date" stroke="#94A3B8" fontSize={10} fontWeight={700} tickLine={false} axisLine={false} />
                      <YAxis stroke="#94A3B8" fontSize={10} fontWeight={700} tickLine={false} axisLine={false} />
                      <Tooltip />
                      <Legend />
                      <Line type="monotone" dataKey="score" stroke="#4F46E5" strokeWidth={2} dot={{ fill: '#4F46E5' }} />
                    </LineChart>
                  </ResponsiveContainer>
                </div>

                {/* Accuracy Trend */}
                <div className="bg-white rounded-2xl border border-gray-200/80 shadow-sm p-6">
                  <h3 className="text-base font-extrabold text-gray-900 mb-6 flex items-center gap-2">
                    <BarChart3 className="w-5 h-5 text-[#10B981]" />
                    Accuracy Trend (30 Days)
                  </h3>
                  <ResponsiveContainer width="100%" height={300}>
                    <AreaChart data={performanceChart}>
                      <CartesianGrid strokeDasharray="3 3" stroke="#F1F5F9" />
                      <XAxis dataKey="date" stroke="#94A3B8" fontSize={10} fontWeight={700} tickLine={false} axisLine={false} />
                      <YAxis stroke="#94A3B8" fontSize={10} fontWeight={700} tickLine={false} axisLine={false} domain={[0, 100]} />
                      <Tooltip />
                      <Legend />
                      <Area type="monotone" dataKey="accuracy" stroke="#10B981" fill="#10B981" fillOpacity={0.3} />
                    </AreaChart>
                  </ResponsiveContainer>
                </div>

                {/* Question Type Distribution */}
                <div className="bg-white rounded-2xl border border-gray-200/80 shadow-sm p-6">
                  <h3 className="text-base font-extrabold text-gray-900 mb-6 flex items-center gap-2">
                    <PieChart className="w-5 h-5 text-[#F59E0B]" />
                    Question Type Distribution
                  </h3>
                  <ResponsiveContainer width="100%" height={300}>
                    <RechartsPieChart>
                      <Pie
                        data={questionTypeDist}
                        cx="50%"
                        cy="50%"
                        innerRadius={60}
                        outerRadius={100}
                        paddingAngle={5}
                        dataKey="count"
                      >
                        {questionTypeDist.map((entry, index) => (
                          <Cell key={`cell-${index}`} fill={COLORS[index % COLORS.length]} />
                        ))}
                      </Pie>
                      <Tooltip />
                      <Legend />
                    </RechartsPieChart>
                  </ResponsiveContainer>
                </div>

                {/* Difficulty Analysis */}
                <div className="bg-white rounded-2xl border border-gray-200/80 shadow-sm p-6">
                  <h3 className="text-base font-extrabold text-gray-900 mb-6 flex items-center gap-2">
                    <Target className="w-5 h-5 text-[#EF4444]" />
                    Difficulty Analysis
                  </h3>
                  <ResponsiveContainer width="100%" height={300}>
                    <BarChart data={difficultyAnalysis}>
                      <CartesianGrid strokeDasharray="3 3" stroke="#F1F5F9" />
                      <XAxis dataKey="difficulty" stroke="#94A3B8" fontSize={10} fontWeight={700} tickLine={false} axisLine={false} />
                      <YAxis stroke="#94A3B8" fontSize={10} fontWeight={700} tickLine={false} axisLine={false} />
                      <Tooltip />
                      <Legend />
                      <Bar dataKey="averageScore" fill="#4F46E5" radius={[4, 4, 0, 0]} />
                    </BarChart>
                  </ResponsiveContainer>
                </div>
              </div>
            )}

            {/* TOPICS SECTION */}
            {activeSection === 'topics' && (
              <div className="bg-white rounded-2xl border border-gray-200/80 shadow-sm p-6">
                <h3 className="text-base font-extrabold text-gray-900 mb-6">Topic Performance</h3>
                <div className="space-y-4">
                  {topicPerformance.map((topic, index) => (
                    <div key={index} className="flex items-center justify-between p-4 bg-gray-50 rounded-xl">
                      <div className="flex items-center gap-4">
                        <div className={`px-3 py-1 rounded-lg text-xs font-bold ${getStatusColor(topic.status)}`}>
                          {topic.status}
                        </div>
                        <div>
                          <p className="text-sm font-bold text-gray-900">{topic.topic}</p>
                          <p className="text-xs text-gray-500">{topic.questionsAttempted} questions attempted</p>
                        </div>
                      </div>
                      <div className="flex items-center gap-4">
                        <div className="text-right">
                          <p className="text-sm font-black text-gray-900">{topic.accuracy.toFixed(1)}%</p>
                          <p className="text-xs text-gray-500">accuracy</p>
                        </div>
                        {topic.status === 'WEAK' && (
                          <button
                            onClick={() => navigate('/practice-center')}
                            className="flex items-center gap-2 px-3 py-1.5 bg-[#4F46E5] text-white text-xs font-bold rounded-lg hover:bg-[#4338CA] transition-all"
                          >
                            Practice Again
                            <ArrowRight className="w-3 h-3" />
                          </button>
                        )}
                      </div>
                    </div>
                  ))}
                </div>
              </div>
            )}

            {/* HEATMAP SECTION */}
            {activeSection === 'heatmap' && (
              <div className="bg-white rounded-2xl border border-gray-200/80 shadow-sm p-6">
                <h3 className="text-base font-extrabold text-gray-900 mb-6 flex items-center gap-2">
                  <Calendar className="w-5 h-5 text-[#4F46E5]" />
                  Study Activity Heatmap (Last 365 Days)
                </h3>
                <div className="grid grid-cols-7 gap-1">
                  {heatmapData.map((day, index) => (
                    <div
                      key={index}
                      className="w-8 h-8 rounded-md cursor-pointer transition-transform hover:scale-110"
                      style={{ backgroundColor: getHeatmapColor(day.activityLevel) }}
                      title={`${day.date}: ${day.testsAttempted} tests, ${day.questionsSolved} questions`}
                    />
                  ))}
                </div>
                <div className="flex items-center justify-end gap-2 mt-4 text-xs text-gray-500">
                  <span>Less</span>
                  {[0, 1, 2, 3, 4, 5].map(level => (
                    <div key={level} className="w-4 h-4 rounded-sm" style={{ backgroundColor: getHeatmapColor(level) }} />
                  ))}
                  <span>More</span>
                </div>
              </div>
            )}

            {/* TESTS SECTION */}
            {activeSection === 'tests' && (
              <div className="bg-white rounded-2xl border border-gray-200/80 shadow-sm overflow-hidden">
                <div className="p-6 border-b border-gray-100">
                  <h3 className="text-base font-extrabold text-gray-900">Test History</h3>
                  {testHistoryError && (
                    <p className="mt-2 text-sm text-red-600">{testHistoryError}</p>
                  )}
                </div>
                <div className="divide-y divide-gray-100">
                  {testHistory.content.map((test, index) => (
                    <div key={index} className="p-4 flex items-center justify-between hover:bg-gray-50 transition-colors">
                      <div className="flex-1">
                        <p className="text-sm font-bold text-gray-900">{test.examType} - {test.questionType}</p>
                        <p className="text-xs text-gray-500">{test.difficulty} • {new Date(test.date).toLocaleDateString()}</p>
                      </div>
                      <div className="flex items-center gap-4">
                        <div className="text-right">
                          <p className="text-sm font-black text-gray-900">{test.percentage.toFixed(1)}%</p>
                          <p className="text-xs text-gray-500">{test.correctQuestions}/{test.totalQuestions} correct</p>
                        </div>
                        <button
                          onClick={() => navigate(`/result/${test.attemptId}`)}
                          className="p-2 text-gray-400 hover:text-[#4F46E5] hover:bg-indigo-50 rounded-lg transition-all"
                        >
                          <Eye className="w-4 h-4" />
                        </button>
                        <button
                          onClick={() => handleDeleteTest(test.attemptId)}
                          className="p-2 text-gray-400 hover:text-red-600 hover:bg-red-50 rounded-lg transition-all"
                        >
                          <Trash2 className="w-4 h-4" />
                        </button>
                      </div>
                    </div>
                  ))}
                </div>
              </div>
            )}

            {/* EVALUATIONS SECTION */}
            {activeSection === 'evaluations' && (
              <div className="bg-white rounded-2xl border border-gray-200/80 shadow-sm overflow-hidden">
                <div className="p-6 border-b border-gray-100 flex items-center justify-between">
                  <h3 className="text-base font-extrabold text-gray-900">Answer Evaluation History</h3>
                  <div className="flex items-center gap-2">
                    <input
                      type="text"
                      placeholder="Search..."
                      value={searchQuery}
                      onChange={(e) => setSearchQuery(e.target.value)}
                      className="px-3 py-1.5 border border-gray-200 rounded-lg text-sm focus:outline-none focus:ring-2 focus:ring-[#4F46E5]"
                    />
                    <button
                      onClick={() => historyAnalyticsApi.getEvaluationHistory(searchQuery, 0, 10).then(setEvaluationHistory)}
                      className="p-2 text-gray-400 hover:text-[#4F46E5] rounded-lg transition-all"
                    >
                      <Search className="w-4 h-4" />
                    </button>
                  </div>
                </div>
                <div className="divide-y divide-gray-100">
                  {evaluationHistory.content.map((evaluationItem, index) => (
                    <div key={index} className="p-4 flex items-center justify-between hover:bg-gray-50 transition-colors">
                      <div className="flex-1">
                        <p className="text-sm font-semibold text-gray-800 line-clamp-1">{evaluationItem.question}</p>
                        <p className="text-xs text-gray-500">{new Date(evaluationItem.createdAt).toLocaleDateString()}</p>
                      </div>
                      <div className="flex items-center gap-4">
                        <div className="text-right">
                          <p className="text-sm font-black text-gray-900">{evaluationItem.marksObtained}/{evaluationItem.maxMarks}</p>
                          <p className="text-xs text-gray-500">{evaluationItem.evaluationStatus}</p>
                        </div>
                        <button
                          onClick={() => handleDeleteEvaluation(evaluationItem.id)}
                          className="p-2 text-gray-400 hover:text-red-600 hover:bg-red-50 rounded-lg transition-all"
                        >
                          <Trash2 className="w-4 h-4" />
                        </button>
                      </div>
                    </div>
                  ))}
                </div>
              </div>
            )}

            {/* INSIGHTS SECTION */}
            {activeSection === 'insights' && (
              <div className="space-y-4">
                {insights.map((insight, index) => {
                  const icon = insight.type === 'STRENGTH' ? CheckCircle2 :
                               insight.type === 'WEAKNESS' ? AlertTriangle :
                               insight.type === 'IMPROVEMENT' ? TrendingUp :
                               Target;
                  const color = insight.type === 'STRENGTH' ? 'emerald' :
                               insight.type === 'WEAKNESS' ? 'red' :
                               insight.type === 'IMPROVEMENT' ? 'blue' :
                               'purple';
                  const colorClasses = {
                    emerald: 'bg-emerald-50 border-emerald-200 text-emerald-800',
                    red: 'bg-red-50 border-red-200 text-red-800',
                    blue: 'bg-blue-50 border-blue-200 text-blue-800',
                    purple: 'bg-purple-50 border-purple-200 text-purple-800',
                  }[color];

                  return (
                    <div key={index} className={`p-4 rounded-xl border ${colorClasses}`}>
                      <div className="flex items-start gap-3">
                        <icon className={`w-5 h-5 mt-0.5 flex-shrink-0 text-${color}-600`} />
                        <div>
                          <p className="text-sm font-bold text-gray-900">{insight.insight}</p>
                          {insight.topic && (
                            <p className="text-xs text-gray-600 mt-1">Topic: {insight.topic}</p>
                          )}
                        </div>
                      </div>
                    </div>
                  );
                })}
              </div>
            )}
          </div>
        </main>
      </div>
    </div>
  );
};

export default HistoryAnalyticsPage;
