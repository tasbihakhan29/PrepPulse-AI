import React, { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import { dashboardService } from '../services/dashboardService';
import logoImg from '../assets/logo.png';
import toast from 'react-hot-toast';
import {
  ResponsiveContainer,
  AreaChart,
  Area,
  XAxis,
  YAxis,
  CartesianGrid,
  Tooltip,
} from 'recharts';
import {
  LayoutDashboard,
  BookOpen,
  Award,
  History,
  LogOut,
  Menu,
  X,
  Flame,
  Brain,
  CheckCircle2,
  TrendingUp,
  Sparkles,
  ArrowUpRight,
  GraduationCap,
  AlertCircle,
  Calendar,
  Lock,
} from 'lucide-react';

const DashboardPage = () => {
  const { user, logout } = useAuth();
  const navigate = useNavigate();
  const [activeTab, setActiveTab] = useState('Dashboard');
  const [sidebarOpen, setSidebarOpen] = useState(false);
  const [dashboardData, setDashboardData] = useState(null);
  const [loadingData, setLoadingData] = useState(true);

  // Fetch Dashboard Stats
  useEffect(() => {
    const fetchDashboardData = async () => {
      try {
        setLoadingData(true);
        const data = await dashboardService.getDashboardData(user);
        setDashboardData(data);
      } catch (err) {
        console.error('Error fetching dashboard details:', err);
        toast.error('Failed to load dashboard statistics.');
      } finally {
        setLoadingData(false);
      }
    };

    if (user) {
      fetchDashboardData();
    }
  }, [user]);

  if (!user) return null;

  const handleLogout = () => {
    logout();
    navigate('/login');
  };

  // Nav items configuration
  const navigationItems = [
    { name: 'Dashboard', icon: LayoutDashboard },
    { name: 'AI Practice Center', icon: BookOpen },
    { name: 'AI Answer Evaluator', icon: Award },
    { name: 'History & Analytics', icon: History },
  ];

  const handleNavigationClick = (itemName) => {
    setActiveTab(itemName);
    setSidebarOpen(false);
  };

  // Custom tooltips for Recharts
  const CustomTooltip = ({ active, payload }) => {
    if (active && payload && payload.length) {
      return (
        <div className="bg-white border border-gray-150 p-3 rounded-xl shadow-lg">
          <p className="text-xs font-semibold text-gray-400">{payload[0].payload.date}</p>
          <p className="text-sm font-black text-[#4F46E5] mt-0.5">
            Score: {payload[0].value}%
          </p>
        </div>
      );
    }
    return null;
  };

  // Render Loader / Skeleton State
  if (loadingData) {
    return (
      <div className="min-h-screen bg-[#F9FAFB] flex font-sans">
        {/* Skeleton Sidebar */}
        <div className="hidden md:flex flex-col w-64 bg-white border-r border-gray-200 p-6 space-y-6">
          <div className="flex items-center gap-3">
            <div className="w-8 h-8 bg-gray-250 rounded-lg animate-pulse" />
            <div className="h-6 w-28 bg-gray-200 rounded animate-pulse" />
          </div>
          <div className="space-y-4 pt-8">
            {[1, 2, 3, 4, 5].map((i) => (
              <div key={i} className="h-11 w-full bg-gray-50 rounded-xl animate-pulse" />
            ))}
          </div>
        </div>
        {/* Skeleton Content Area */}
        <div className="flex-1 flex flex-col">
          <div className="h-16 bg-white border-b border-gray-200 px-8 flex items-center justify-between">
            <div className="h-6 w-24 bg-gray-100 rounded animate-pulse" />
            <div className="h-10 w-48 bg-gray-100 rounded-full animate-pulse" />
          </div>
          <div className="p-6 md:p-8 space-y-8 flex-1 overflow-y-auto">
            <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-5 gap-4">
              {[1, 2, 3, 4, 5].map((i) => (
                <div key={i} className="h-24 bg-white rounded-2xl border border-gray-200/80 p-6 animate-pulse" />
              ))}
            </div>
            <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
              <div className="lg:col-span-2 h-[380px] bg-white rounded-2xl border border-gray-200/80 p-6 animate-pulse" />
              <div className="h-[380px] bg-white rounded-2xl border border-gray-200/80 p-6 animate-pulse" />
            </div>
          </div>
        </div>
      </div>
    );
  }

  // Define components for stats mapping
  const statCardConfigs = [
    {
      title: 'Total Tests Taken',
      value: dashboardData.stats.totalTests,
      icon: GraduationCap,
      color: 'bg-indigo-50 text-indigo-650 border-indigo-100/50',
    },
    {
      title: 'Average Score',
      value: dashboardData.stats.totalTests > 0 ? dashboardData.stats.averageScore : '0',
      suffix: dashboardData.stats.totalTests > 0 ? '%' : '',
      icon: Award,
      color: 'bg-emerald-50 text-emerald-650 border-emerald-100/50',
    },
    {
      title: 'Study Streak 🔥',
      value: dashboardData.stats.studyStreak,
      suffix: ' days',
      icon: Flame,
      color: 'bg-orange-50 text-orange-650 border-orange-100/50',
    },
    {
      title: 'Flashcards Generated',
      value: dashboardData.stats.flashcardsGenerated,
      icon: Brain,
      color: 'bg-blue-50 text-blue-650 border-blue-100/50',
    },
    {
      title: 'AI Evaluations Done',
      value: dashboardData.stats.aiEvaluations,
      icon: CheckCircle2,
      color: 'bg-rose-50 text-rose-650 border-rose-100/50',
    },
  ];

  return (
    <div className="min-h-screen bg-[#F9FAFB] flex font-sans antialiased">
      
      {/* 1. DESKTOP SIDEBAR */}
      <aside className="hidden md:flex flex-col w-64 bg-white border-r border-gray-200 h-screen fixed left-0 top-0 z-30">
        <div className="flex items-center gap-3 px-6 h-16 border-b border-gray-100 flex-shrink-0">
          <img src={logoImg} alt="PrepPulse AI Logo" className="w-8 h-8 object-contain" />
          <span className="text-lg font-black text-gray-900 tracking-tight">PrepPulse AI</span>
        </div>
        
        {/* Navigation list */}
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

        {/* Logout button at bottom of sidebar */}
        <div className="p-4 border-t border-gray-100">
          <button
            onClick={handleLogout}
            className="w-full flex items-center gap-3 px-4 py-3 rounded-xl text-sm font-bold text-gray-500 hover:bg-red-50 hover:text-[#EF4444] transition-all duration-150"
          >
            <LogOut className="w-5 h-5 text-gray-400 group-hover:text-[#EF4444]" />
            <span>Logout</span>
          </button>
        </div>
      </aside>

      {/* 2. MOBILE SIDEBAR DRAWER */}
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

      {/* 3. MAIN WORKSPACE CONTENT */}
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

          {/* Right Side - State-aware Greeting and User Info */}
          <div className="flex items-center gap-4 ml-auto">
            <div className="text-right hidden sm:flex flex-col justify-center">
              {user.onboardingCompleted === false ? (
                <>
                  <span className="text-[10px] text-indigo-600 font-extrabold tracking-wider uppercase leading-none mb-0.5">Welcome to PrepPulse, {user.name}!</span>
                  <span className="text-xs font-bold text-gray-500">Let's set up your first test.</span>
                </>
              ) : (
                <>
                  <span className="text-[10px] text-[#10B981] font-extrabold tracking-wider uppercase leading-none mb-0.5">Ready to Learn</span>
                  <span className="text-xs font-bold text-gray-800">Welcome back, {user.name}!</span>
                </>
              )}
            </div>

            {/* Profile Avatar Card */}
            <div className="flex items-center">
              {user.profilePicture ? (
                <img
                  src={user.profilePicture}
                  alt={user.name}
                  className="w-9 h-9 rounded-full border-2 border-indigo-100 object-cover shadow-sm select-none"
                  referrerPolicy="no-referrer"
                />
              ) : (
                <div className="w-9 h-9 rounded-full bg-indigo-600 border border-indigo-700 flex items-center justify-center text-white font-extrabold text-xs shadow-sm select-none">
                  {user.name.charAt(0).toUpperCase()}
                </div>
              )}
            </div>
          </div>
        </header>

        {/* WORKSPACE AREA */}
        <main className="flex-1 p-6 md:p-8 bg-[#F9FAFB] space-y-8 overflow-y-auto">
          
          {activeTab === 'Dashboard' ? (
            <>
              {/* ONBOARDING EMPTY STATE EXPERIENCE */}
              {dashboardData.stats.totalTests === 0 && (
                <div className="bg-gradient-to-r from-indigo-500 via-indigo-650 to-indigo-700 text-white rounded-3xl p-6 sm:p-8 shadow-xl shadow-indigo-100/50 relative overflow-hidden group">
                  <div className="absolute right-[-5%] top-[-20%] w-60 h-60 bg-white/10 rounded-full blur-2xl group-hover:scale-110 transition-transform duration-700" />
                  <div className="absolute right-[25%] bottom-[-40%] w-44 h-44 bg-[#10B981]/15 rounded-full blur-xl group-hover:scale-110 transition-transform duration-700" />
                  
                  <div className="relative z-10 flex flex-col lg:flex-row lg:items-center justify-between gap-6">
                    <div className="max-w-xl">
                      <div className="inline-flex items-center gap-1.5 px-3 py-1 rounded-full bg-white/10 border border-white/20 text-[10px] font-black tracking-wider uppercase mb-4">
                        <Sparkles className="w-3.5 h-3.5 text-[#10B981]" />
                        <span>Get Started</span>
                      </div>
                      <h2 className="text-2xl sm:text-3xl font-black tracking-tight mb-2">
                        Start Your First AI Practice Test
                      </h2>
                      <p className="text-indigo-100 text-sm sm:text-base font-semibold leading-relaxed">
                        Upload notes and generate personalized tests instantly.
                      </p>
                    </div>
                    <div className="flex-shrink-0">
                      <button
                        onClick={() => setActiveTab('AI Practice Center')}
                        className="w-full sm:w-auto inline-flex items-center justify-center px-6 py-4 bg-white text-[#4F46E5] hover:bg-indigo-50 active:scale-[0.98] text-sm font-black rounded-2xl transition-all duration-150 shadow-md hover:shadow-lg"
                      >
                        Go To AI Practice Center
                        <ArrowUpRight className="ml-2 w-4.5 h-4.5" />
                      </button>
                    </div>
                  </div>
                </div>
              )}

              {/* STAT CARDS ROW */}
              <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-5 gap-5">
                {statCardConfigs.map((card, idx) => {
                  const Icon = card.icon;
                  return (
                    <div
                      key={idx}
                      className="bg-white p-6 rounded-2xl border border-gray-200/80 shadow-sm hover:shadow-md hover:-translate-y-1 hover:border-indigo-100 transition-all duration-300 flex items-center justify-between group"
                    >
                      <div className="space-y-1">
                        <span className="block text-[10px] font-black text-gray-400 uppercase tracking-wider">
                          {card.title}
                        </span>
                        <h3 className="text-2xl font-black text-gray-900 tracking-tight leading-none">
                          {card.value}
                          {card.suffix && <span className="text-xs text-gray-500 font-bold ml-0.5">{card.suffix}</span>}
                        </h3>
                      </div>
                      <div className={`w-12 h-12 rounded-xl flex items-center justify-center border transition-transform group-hover:scale-105 duration-300 ${card.color}`}>
                        <Icon className="w-6 h-6" />
                      </div>
                    </div>
                  );
                })}
              </div>

              {/* DUAL SECTION LAYOUT (CHART & INSIGHTS) */}
              <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
                
                {/* CHART CARD */}
                <div className="bg-white p-6 rounded-2xl border border-gray-200/80 shadow-sm lg:col-span-2 flex flex-col justify-between">
                  <div className="flex items-center justify-between mb-6">
                    <div className="space-y-0.5">
                      <h3 className="text-base font-extrabold text-gray-900">Performance Trend</h3>
                      <p className="text-xs text-gray-400 font-medium">Display last 5 tests score percentages</p>
                    </div>
                    <div className="p-2 bg-indigo-50 border border-indigo-100/30 rounded-lg text-[#4F46E5]">
                      <TrendingUp className="w-4 h-4" />
                    </div>
                  </div>

                  {dashboardData.performanceTrend.length === 0 ? (
                    <div className="flex-1 flex flex-col items-center justify-center h-[260px] border border-dashed border-gray-250/70 rounded-2xl bg-gray-50/50 p-6 text-center select-none">
                      <TrendingUp className="w-10 h-10 text-gray-300 mb-2" />
                      <h4 className="text-sm font-bold text-gray-800">No Performance History</h4>
                      <p className="text-xs text-gray-400 max-w-xs mt-1">
                        Plotting requires score records. Complete your first practice exam to chart progress.
                      </p>
                    </div>
                  ) : (
                    <div className="w-full overflow-hidden">
                      <ResponsiveContainer width="100%" height={260}>
                        <AreaChart
                          data={dashboardData.performanceTrend}
                          margin={{ top: 10, right: 10, left: -25, bottom: 0 }}
                        >
                          <defs>
                            <linearGradient id="colorScore" x1="0" y1="0" x2="0" y2="1">
                              <stop offset="5%" stopColor="#4F46E5" stopOpacity={0.15} />
                              <stop offset="95%" stopColor="#4F46E5" stopOpacity={0} />
                            </linearGradient>
                          </defs>
                          <CartesianGrid strokeDasharray="3 3" stroke="#F1F5F9" vertical={false} />
                          <XAxis
                            dataKey="name"
                            stroke="#94A3B8"
                            fontSize={10}
                            fontWeight={700}
                            tickLine={false}
                            axisLine={false}
                            dy={10}
                          />
                          <YAxis
                            stroke="#94A3B8"
                            fontSize={10}
                            fontWeight={700}
                            tickLine={false}
                            axisLine={false}
                            domain={[0, 100]}
                            dx={-5}
                          />
                          <Tooltip content={<CustomTooltip />} />
                          <Area
                            type="monotone"
                            dataKey="score"
                            stroke="#4F46E5"
                            strokeWidth={3.5}
                            fillOpacity={1}
                            fill="url(#colorScore)"
                          />
                        </AreaChart>
                      </ResponsiveContainer>
                    </div>
                  )}
                </div>

                {/* AI INSIGHTS CARD */}
                <div className="bg-white p-6 rounded-2xl border border-gray-200/80 shadow-sm flex flex-col">
                  <div className="flex items-center justify-between mb-6">
                    <div className="space-y-0.5">
                      <h3 className="text-base font-extrabold text-gray-900">Learning Insights</h3>
                      <p className="text-xs text-gray-400 font-medium">AI generated subject analysis</p>
                    </div>
                    <div className="p-2 bg-indigo-50 border border-indigo-100/30 rounded-lg text-[#4F46E5]">
                      <Sparkles className="w-4 h-4" />
                    </div>
                  </div>

                  {dashboardData.stats.totalTests === 0 ? (
                    <div className="flex-1 flex flex-col items-center justify-center border border-dashed border-gray-250/70 rounded-2xl bg-gray-50/50 p-6 text-center select-none">
                      <Sparkles className="w-8 h-8 text-indigo-200 mb-2" />
                      <p className="text-xs font-bold text-gray-500 leading-relaxed max-w-[200px]">
                        Take your first test to unlock AI insights.
                      </p>
                    </div>
                  ) : (
                    <div className="flex-1 flex flex-col justify-between space-y-6">
                      {/* Strong Topics */}
                      <div className="space-y-2.5">
                        <span className="block text-[10px] font-black text-[#10B981] uppercase tracking-wider">
                          Strong Areas
                        </span>
                        <div className="flex flex-wrap gap-2">
                          {dashboardData.insights.strongTopics.map((topic, idx) => (
                            <span
                              key={idx}
                              className="inline-flex items-center px-3 py-1.5 rounded-xl text-xs font-bold bg-emerald-50 text-emerald-700 border border-emerald-100/30"
                            >
                              <CheckCircle2 className="w-3.5 h-3.5 mr-1.5 text-emerald-500 flex-shrink-0" />
                              {topic}
                            </span>
                          ))}
                        </div>
                      </div>

                      {/* Weak Topics */}
                      <div className="space-y-2.5">
                        <span className="block text-[10px] font-black text-orange-650 uppercase tracking-wider">
                          Needs Improvement
                        </span>
                        <div className="flex flex-wrap gap-2">
                          {dashboardData.insights.weakTopics.map((topic, idx) => (
                            <span
                              key={idx}
                              className="inline-flex items-center px-3 py-1.5 rounded-xl text-xs font-bold bg-orange-50/70 text-orange-700 border border-orange-100/30"
                            >
                              <AlertCircle className="w-3.5 h-3.5 mr-1.5 text-orange-500 flex-shrink-0" />
                              {topic}
                            </span>
                          ))}
                        </div>
                      </div>
                    </div>
                  )}
                </div>
              </div>

              {/* RECENT ACTIVITY */}
              <div className="bg-white rounded-2xl border border-gray-200/80 shadow-sm overflow-hidden">
                <div className="px-6 py-5 border-b border-gray-100 flex items-center justify-between">
                  <div>
                    <h3 className="text-base font-extrabold text-gray-900">Recent Activity</h3>
                    <p className="text-xs text-gray-400 font-medium">Overview of your recent practice tests</p>
                  </div>
                  <span className="px-3 py-1 bg-gray-50 border border-gray-150 rounded-xl text-xs font-extrabold text-gray-500">
                    Recent Tests
                  </span>
                </div>

                {dashboardData.recentActivity.length === 0 ? (
                  <div className="p-8 text-center text-xs text-gray-400 font-semibold select-none">
                    No recent activities recorded. Complete a quiz to see evaluations here.
                  </div>
                ) : (
                  <div className="overflow-x-auto">
                    <table className="min-w-full divide-y divide-gray-100">
                      <thead className="bg-gray-50/50">
                        <tr>
                          <th className="px-6 py-4 text-left text-[10px] font-black text-gray-400 uppercase tracking-wider">
                            Test Name
                          </th>
                          <th className="px-6 py-4 text-left text-[10px] font-black text-gray-400 uppercase tracking-wider">
                            Score
                          </th>
                          <th className="px-6 py-4 text-left text-[10px] font-black text-gray-400 uppercase tracking-wider">
                            Date
                          </th>
                        </tr>
                      </thead>
                      <tbody className="bg-white divide-y divide-gray-100">
                        {dashboardData.recentActivity.map((activity) => (
                          <tr key={activity.id} className="hover:bg-gray-50/30 transition-colors">
                            <td className="px-6 py-4.5 whitespace-nowrap text-xs font-bold text-gray-900">
                              {activity.name}
                            </td>
                            <td className="px-6 py-4.5 whitespace-nowrap">
                              <span
                                className={`inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-bold ${
                                  activity.score >= 85
                                    ? 'bg-emerald-50 text-emerald-700'
                                    : activity.score >= 70
                                    ? 'bg-indigo-50/70 text-indigo-700'
                                    : 'bg-orange-50 text-orange-700'
                                }`}
                              >
                                {activity.score}%
                              </span>
                            </td>
                            <td className="px-6 py-4.5 whitespace-nowrap text-xs text-gray-400 font-bold">
                              {activity.date}
                            </td>
                          </tr>
                        ))}
                      </tbody>
                    </table>
                  </div>
                )}
              </div>
            </>
          ) : (
            /* COMING SOON PLACEHOLDER (No external pages) */
            <div className="flex flex-col items-center justify-center min-h-[55vh] bg-white rounded-3xl border border-gray-200/80 p-8 sm:p-16 text-center shadow-sm">
              <div className="w-16 h-16 bg-indigo-50 border border-indigo-100/35 rounded-2xl flex items-center justify-center text-[#4F46E5] mb-6">
                <Lock className="w-6 h-6 animate-pulse" />
              </div>
              <h2 className="text-2xl font-black text-gray-900 tracking-tight mb-2">
                {activeTab} is Coming Soon
              </h2>
              <p className="text-gray-450 text-sm font-semibold max-w-sm mb-8 leading-relaxed">
                We are currently building this feature to integrate advanced AI models for your study path. It will be available shortly!
              </p>
              <button
                onClick={() => setActiveTab('Dashboard')}
                className="inline-flex items-center justify-center px-5 py-3 bg-[#4F46E5] text-white text-sm font-black rounded-xl hover:bg-[#4338CA] active:scale-[0.98] transition-all shadow-md shadow-indigo-100"
              >
                Return to Dashboard
              </button>
            </div>
          )}
        </main>
      </div>
    </div>
  );
};

export default DashboardPage;
