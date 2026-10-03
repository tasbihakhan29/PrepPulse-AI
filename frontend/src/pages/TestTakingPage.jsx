import React, { useState, useEffect, useRef, useCallback } from 'react';
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
  AlertTriangle,
  CheckCircle2,
  XCircle,
  Flag,
  RotateCcw,
  Send,
} from 'lucide-react';

const TestTakingPage = () => {
  const { user, logout } = useAuth();
  const navigate = useNavigate();
  const { testId } = useParams();

  const [sidebarOpen, setSidebarOpen] = useState(false);
  const [activeTab, setActiveTab] = useState('Live Test');

  // Test state
  const [testData, setTestData] = useState(null);
  const [loading, setLoading] = useState(true);
  const [currentQuestionIndex, setCurrentQuestionIndex] = useState(0);
  const [answers, setAnswers] = useState({});
  const [visitedQuestions, setVisitedQuestions] = useState(new Set());
  const [markedForReview, setMarkedForReview] = useState(new Set());
  const [submitting, setSubmitting] = useState(false);
  const [showSubmitDialog, setShowSubmitDialog] = useState(false);
  const [submitSummary, setSubmitSummary] = useState(null);

  // Timer state
  const [timeLeft, setTimeLeft] = useState(0);
  const [timeWarning, setTimeWarning] = useState(null);

  // Tab switch detection
  const [tabSwitchCount, setTabSwitchCount] = useState(0);
  const [showTabWarning, setShowTabWarning] = useState(false);
  const [tabSwitchEvents, setTabSwitchEvents] = useState([]);

  // Auto-save debounce
  const saveTimeoutRef = useRef(null);
  const timerRef = useRef(null);
  const hiddenSinceRef = useRef(null);
  const autoSubmitTriggeredRef = useRef(false);
  const submittingRef = useRef(false);

  // Load test
  useEffect(() => {
    const loadTest = async () => {
      try {
        setLoading(true);
        const response = await testTakingApi.startTest(testId);
        setTestData(response);
        setCurrentQuestionIndex(0);

        const savedAnswers = response.savedAnswers || {};
        setAnswers(savedAnswers);

        const visited = new Set();
        (response.questions || []).forEach((question, index) => {
          if (savedAnswers[question.id] !== undefined && savedAnswers[question.id] !== null && savedAnswers[question.id] !== '') {
            visited.add(index);
          }
        });
        if ((response.questions || []).length > 0) {
          visited.add(0);
        }
        setVisitedQuestions(visited);
        setMarkedForReview(new Set());
        autoSubmitTriggeredRef.current = false;
        submittingRef.current = false;
        
        // Calculate time left based on duration
        const startTime = new Date(response.startTime);
        const duration = response.duration || 3600; // Default 60 minutes
        const elapsed = Math.floor((Date.now() - startTime.getTime()) / 1000);
        const remaining = Math.max(0, duration - elapsed);
        setTimeLeft(remaining);

        setLoading(false);
      } catch (err) {
        console.error('Error loading test:', err);
        toast.error('Failed to load test. Please try again.');
        navigate('/practice-center');
      }
    };

    if (user) {
      loadTest();
    }
  }, [user, testId, navigate]);

  // Timer
  useEffect(() => {
    if (!testData || submitting) {
      return;
    }

    timerRef.current = setInterval(() => {
      setTimeLeft((prev) => {
        if (prev <= 1) {
          if (!autoSubmitTriggeredRef.current) {
            autoSubmitTriggeredRef.current = true;
            void submitTest(true);
          }
          return 0;
        }
        if (prev === 300) {
          setTimeWarning('five');
          setTimeout(() => setTimeWarning(null), 5000);
        }
        if (prev === 60) {
          setTimeWarning('one');
          setTimeout(() => setTimeWarning(null), 5000);
        }
        return prev - 1;
      });
    }, 1000);

    return () => {
      clearInterval(timerRef.current);
      timerRef.current = null;
    };
  }, [timeLeft, submitting, testData]);

  // Tab switch detection
  useEffect(() => {
    const handleVisibilityChange = () => {
      if (document.hidden) {
        hiddenSinceRef.current = Date.now();
      } else if (hiddenSinceRef.current) {
        const durationMs = Date.now() - hiddenSinceRef.current;
        hiddenSinceRef.current = null;

        setTabSwitchCount((prev) => {
          const newCount = prev + 1;
          setTabSwitchEvents((current) => [
            ...current,
            {
              timestamp: new Date().toISOString(),
              durationMs,
              switchCount: newCount,
            },
          ]);
          if (newCount >= 3) {
            setShowTabWarning(true);
            setTimeout(() => setShowTabWarning(false), 5000);
          }
          return newCount;
        });
      }
    };

    document.addEventListener('visibilitychange', handleVisibilityChange);
    return () => document.removeEventListener('visibilitychange', handleVisibilityChange);
  }, []);

  // Mark current question as visited
  useEffect(() => {
    if (testData && testData.questions) {
      setVisitedQuestions((prev) => new Set([...prev, currentQuestionIndex]));
    }
  }, [currentQuestionIndex, testData]);

  // Auto-save answer with debounce
  const saveAnswer = useCallback(
    async (questionId, answer) => {
      if (saveTimeoutRef.current) {
        clearTimeout(saveTimeoutRef.current);
      }

      saveTimeoutRef.current = setTimeout(async () => {
        try {
          await testTakingApi.saveAnswer(testData.attemptId, questionId, answer);
        } catch (err) {
          console.error('Error saving answer:', err);
        }
      }, 1000); // 1 second debounce
    },
    [testData]
  );

  const submitTest = useCallback(async (forceSubmit = false) => {
    if (!testData || submitting || submittingRef.current) {
      return;
    }

    const totalQuestions = testData?.questions?.length || 0;
    const answeredCount = Object.keys(answers).filter((questionId) => {
      const value = answers[questionId];
      return value !== undefined && value !== null && value !== '';
    }).length;
    const unansweredCount = totalQuestions - answeredCount;
    const reviewCount = markedForReview.size;

    if (!forceSubmit && (unansweredCount > 0 || reviewCount > 0)) {
      setSubmitSummary({ answeredCount, unansweredCount, reviewCount });
      setShowSubmitDialog(true);
      return;
    }

    submittingRef.current = true;
    clearInterval(timerRef.current);
    timerRef.current = null;
    setSubmitting(true);
    setShowSubmitDialog(false);

    try {
      const result = await testTakingApi.submitTest(testData.attemptId, tabSwitchCount);
      navigate(`/result/${result.attemptId || testData.attemptId}`);
    } catch (err) {
      console.error('Error submitting test:', err);
      toast.error('Failed to submit test. Please try again.');
      submittingRef.current = false;
      setSubmitting(false);
      autoSubmitTriggeredRef.current = false;
    }
  }, [answers, markedForReview.size, navigate, submitting, tabSwitchCount, testData]);

  const handleAnswerChange = (questionId, answer) => {
    setAnswers((prev) => ({ ...prev, [questionId]: answer }));
    saveAnswer(questionId, answer);
  };

  const handleMultipleAnswerChange = (questionId, optionLetter) => {
    setAnswers((prev) => {
      const current = prev[questionId] ? prev[questionId].split(',').map((item) => item.trim()).filter(Boolean) : [];
      const next = current.includes(optionLetter)
        ? current.filter((item) => item !== optionLetter)
        : [...current, optionLetter];
      const normalized = next.sort().join(',');
      saveAnswer(questionId, normalized);
      return { ...prev, [questionId]: normalized };
    });
  };

  const handleNumericalAnswerChange = (questionId, value) => {
    const normalized = value === '' ? '' : value;
    setAnswers((prev) => ({ ...prev, [questionId]: normalized }));
    saveAnswer(questionId, normalized);
  };

  const handleLogout = () => {
    logout();
    navigate('/login');
  };

  const formatTime = (seconds) => {
    const hours = Math.floor(seconds / 3600);
    const minutes = Math.floor((seconds % 3600) / 60);
    const secs = seconds % 60;
    return `${hours.toString().padStart(2, '0')} : ${minutes.toString().padStart(2, '0')} : ${secs.toString().padStart(2, '0')}`;
  };

  const handleNext = () => {
    if (currentQuestionIndex < (testData?.questions?.length || 0) - 1) {
      setCurrentQuestionIndex((prev) => prev + 1);
    }
  };

  const handlePrevious = () => {
    if (currentQuestionIndex > 0) {
      setCurrentQuestionIndex((prev) => prev - 1);
    }
  };

  const handleMarkForReview = () => {
    setMarkedForReview((prev) => {
      const newSet = new Set(prev);
      if (newSet.has(currentQuestionIndex)) {
        newSet.delete(currentQuestionIndex);
      } else {
        newSet.add(currentQuestionIndex);
      }
      return newSet;
    });
  };

  const handleClearResponse = () => {
    const currentQuestion = testData?.questions[currentQuestionIndex];
    if (currentQuestion) {
      setAnswers((prev) => {
        const newAnswers = { ...prev };
        delete newAnswers[currentQuestion.id];
        return newAnswers;
      });
      saveAnswer(currentQuestion.id, null);
    }
  };

  const handleJumpToQuestion = (index) => {
    setCurrentQuestionIndex(index);
  };

  const handleSubmit = () => {
    void submitTest(false);
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

  const getQuestionStatus = (index) => {
    const question = testData?.questions[index];
    const hasAnswer = question && answers[question.id] !== undefined && answers[question.id] !== null && answers[question.id] !== '';
    const isMarked = markedForReview.has(index);
    const isVisited = visitedQuestions.has(index);

    if (isMarked) return 'review';
    if (hasAnswer) return 'answered';
    if (isVisited) return 'visited';
    return 'not-visited';
  };

  const currentQuestion = testData?.questions[currentQuestionIndex];
  const currentQuestionType = currentQuestion?.questionType || testData?.questionType || 'MCQ';

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
        
        {/* TOP BAR WITH TIMER */}
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

          {/* TIMER */}
          <div className={`flex items-center gap-2 px-4 py-2 rounded-xl font-mono font-bold text-lg ${
            timeLeft <= 300 ? 'bg-red-50 text-red-600' : 'bg-indigo-50 text-[#4F46E5]'
          }`}>
            <Clock className="w-5 h-5" />
            <span>{formatTime(timeLeft)}</span>
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

        {/* TEST INTERFACE */}
        <main className="flex-1 bg-[#F9FAFB] overflow-hidden">
          <div className="h-full flex">
            
            {/* LEFT - QUESTION PALETTE */}
            <div className="hidden lg:flex w-72 flex-col border-r border-gray-200 bg-white overflow-y-auto">
              <div className="p-4 border-b border-gray-100">
                <h3 className="text-sm font-extrabold text-gray-900 mb-3">Question Palette</h3>
                <div className="flex flex-wrap gap-2 text-xs">
                  <div className="flex items-center gap-1">
                    <div className="w-4 h-4 bg-gray-200 rounded"></div>
                    <span className="text-gray-600">Not Visited</span>
                  </div>
                  <div className="flex items-center gap-1">
                    <div className="w-4 h-4 bg-blue-100 rounded"></div>
                    <span className="text-gray-600">Visited</span>
                  </div>
                  <div className="flex items-center gap-1">
                    <div className="w-4 h-4 bg-orange-500 rounded"></div>
                    <span className="text-gray-600">Answered</span>
                  </div>
                  <div className="flex items-center gap-1">
                    <div className="w-4 h-4 bg-green-500 rounded"></div>
                    <span className="text-gray-600">Review</span>
                  </div>
                </div>
              </div>

              <div className="p-4 grid grid-cols-5 gap-2">
                {testData?.questions?.map((_, index) => {
                  const status = getQuestionStatus(index);
                  const bgColor = {
                    'not-visited': 'bg-gray-200 text-gray-600',
                    'visited': 'bg-blue-100 text-blue-700',
                    'answered': 'bg-orange-500 text-white',
                    'review': 'bg-green-500 text-white',
                  }[status];

                  return (
                    <button
                      key={index}
                      onClick={() => handleJumpToQuestion(index)}
                      disabled={submitting}
                      className={`w-10 h-10 rounded-lg font-bold text-sm transition-all hover:scale-105 ${bgColor} ${
                        currentQuestionIndex === index ? 'ring-2 ring-[#4F46E5] ring-offset-2' : ''
                      }`}
                    >
                      {index + 1}
                    </button>
                  );
                })}
              </div>
            </div>

            {/* RIGHT - QUESTION AREA */}
            <div className="flex-1 flex flex-col overflow-hidden">
              <div className="flex-1 overflow-y-auto p-6 md:p-8">
                {currentQuestion && (
                  <div className="max-w-4xl mx-auto space-y-6">
                    
                    {/* Question Header */}
                    <div className="flex items-center justify-between">
                      <div className="flex items-center gap-3">
                        <span className="px-3 py-1 bg-[#4F46E5] text-white text-sm font-black rounded-lg">
                          Question {currentQuestionIndex + 1}
                        </span>
                        {currentQuestion.topic && (
                          <span className="px-3 py-1 bg-gray-100 text-gray-700 text-xs font-bold rounded-lg">
                            {currentQuestion.topic}
                          </span>
                        )}
                        {currentQuestion.difficulty && (
                          <span className="px-3 py-1 bg-gray-100 text-gray-700 text-xs font-bold rounded-lg">
                            {currentQuestion.difficulty}
                          </span>
                        )}
                      </div>
                      <button
                        onClick={handleMarkForReview}
                        disabled={submitting}
                        className={`flex items-center gap-2 px-3 py-1.5 rounded-lg text-sm font-bold transition-all ${
                          markedForReview.has(currentQuestionIndex)
                            ? 'bg-green-500 text-white'
                            : 'bg-gray-100 text-gray-700 hover:bg-gray-200'
                        }`}
                      >
                        <Flag className="w-4 h-4" />
                        Mark for Review
                      </button>
                    </div>

                    {/* Question Text */}
                    <div className="bg-white rounded-2xl border border-gray-200 p-6 shadow-sm">
                      <p className="text-base font-semibold text-gray-800 leading-relaxed">
                        {currentQuestion.question}
                      </p>
                    </div>

                    {/* Answer Area */}
                    <div className="bg-white rounded-2xl border border-gray-200 p-6 shadow-sm space-y-3">
                      {currentQuestionType === 'Numerical' ? (
                        <div className="space-y-3">
                          <label className="block text-sm font-bold text-gray-700">Enter numeric answer</label>
                          <input
                            type="number"
                            step="any"
                            inputMode="decimal"
                            value={answers[currentQuestion.id] ?? ''}
                            disabled={submitting}
                            onChange={(event) => handleNumericalAnswerChange(currentQuestion.id, event.target.value)}
                            placeholder="Type your answer"
                            className="w-full rounded-xl border border-gray-200 px-4 py-3 text-gray-900 font-semibold focus:outline-none focus:ring-2 focus:ring-[#4F46E5] focus:border-transparent"
                          />
                          <p className="text-xs text-gray-500">Decimals are allowed.</p>
                        </div>
                      ) : (
                        currentQuestion.options?.map((option, index) => {
                          const optionLetter = String.fromCharCode(65 + index);
                          const selectedValue = answers[currentQuestion.id] || '';
                          const selectedSet = selectedValue
                            ? selectedValue.split(',').map((item) => item.trim()).filter(Boolean)
                            : [];
                          const isSelected = currentQuestionType === 'MSQ'
                            ? selectedSet.includes(optionLetter)
                            : selectedValue === optionLetter;

                          return (
                            <label
                              key={index}
                              className={`flex items-center gap-4 p-4 rounded-xl border-2 cursor-pointer transition-all ${
                                isSelected
                                  ? 'border-[#4F46E5] bg-indigo-50'
                                  : 'border-gray-200 hover:border-gray-300'
                              }`}
                            >
                              <input
                                type={currentQuestionType === 'MSQ' ? 'checkbox' : 'radio'}
                                name={`question-${currentQuestion.id}`}
                                value={optionLetter}
                                checked={isSelected}
                                disabled={submitting}
                                onChange={() => (
                                  currentQuestionType === 'MSQ'
                                    ? handleMultipleAnswerChange(currentQuestion.id, optionLetter)
                                    : handleAnswerChange(currentQuestion.id, optionLetter)
                                )}
                                className="w-5 h-5 text-[#4F46E5]"
                              />
                              <span className="w-8 h-8 flex items-center justify-center bg-gray-100 rounded-lg font-bold text-gray-700">
                                {optionLetter}
                              </span>
                              <span className="text-sm font-medium text-gray-800">{option}</span>
                            </label>
                          );
                        })
                      )}
                    </div>
                  </div>
                )}
              </div>

              {/* BOTTOM CONTROLS */}
              <div className="border-t border-gray-200 bg-white p-4">
                <div className="max-w-4xl mx-auto flex items-center justify-between">
                  <div className="flex items-center gap-3">
                    <button
                      onClick={handlePrevious}
                      disabled={submitting || currentQuestionIndex === 0}
                      className="flex items-center gap-2 px-4 py-2.5 border border-gray-200 rounded-xl text-sm font-bold text-gray-700 hover:bg-gray-50 disabled:opacity-50 disabled:cursor-not-allowed transition-all"
                    >
                      <ChevronLeft className="w-4 h-4" />
                      Previous
                    </button>
                    <button
                      onClick={handleClearResponse}
                      disabled={submitting}
                      className="flex items-center gap-2 px-4 py-2.5 border border-gray-200 rounded-xl text-sm font-bold text-gray-700 hover:bg-gray-50 transition-all"
                    >
                      <RotateCcw className="w-4 h-4" />
                      Clear Response
                    </button>
                  </div>

                  <div className="flex items-center gap-3">
                    <button
                      onClick={handleNext}
                      disabled={submitting || currentQuestionIndex >= (testData?.questions?.length || 0) - 1}
                      className="flex items-center gap-2 px-4 py-2.5 border border-gray-200 rounded-xl text-sm font-bold text-gray-700 hover:bg-gray-50 disabled:opacity-50 disabled:cursor-not-allowed transition-all"
                    >
                      Next
                      <ChevronRight className="w-4 h-4" />
                    </button>
                    <button
                      onClick={handleSubmit}
                      disabled={submitting}
                      className="flex items-center gap-2 px-6 py-2.5 bg-[#10B981] hover:bg-[#059669] disabled:bg-gray-300 disabled:cursor-not-allowed text-white text-sm font-bold rounded-xl transition-all"
                    >
                      {submitting ? (
                        'Submitting...'
                      ) : (
                        <>
                          <Send className="w-4 h-4" />
                          Submit Test
                        </>
                      )}
                    </button>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </main>
      </div>

      {submitting && (
        <div className="fixed inset-0 z-[70] flex items-center justify-center bg-gray-950/45 px-4">
          <div className="w-full max-w-sm rounded-3xl bg-white p-8 text-center shadow-2xl">
            <div className="mx-auto mb-5 h-10 w-10 animate-spin rounded-full border-4 border-indigo-100 border-t-[#4F46E5]" />
            <h3 className="text-xl font-black text-gray-900">Submitting your test...</h3>
            <p className="mt-2 text-sm font-semibold text-gray-500">Checking your answers and calculating your result.</p>
          </div>
        </div>
      )}

      {/* WARNINGS */}
      {timeWarning && (
        <div className="fixed top-20 right-6 bg-red-50 border border-red-200 rounded-xl p-4 shadow-lg z-50 animate-pulse">
          <div className="flex items-center gap-3">
            <AlertTriangle className="w-5 h-5 text-red-600" />
            <div>
              <p className="text-sm font-bold text-red-800">Time Warning</p>
              <p className="text-xs text-red-600">
                {timeWarning === 'one' ? 'Less than 1 minute remaining!' : 'Less than 5 minutes remaining!'}
              </p>
            </div>
          </div>
        </div>
      )}

      {showTabWarning && (
        <div className="fixed top-20 right-6 bg-orange-50 border border-orange-200 rounded-xl p-4 shadow-lg z-50">
          <div className="flex items-center gap-3">
            <AlertTriangle className="w-5 h-5 text-orange-600" />
            <div>
              <p className="text-sm font-bold text-orange-800">Tab Switch Detected</p>
              <p className="text-xs text-orange-600">Switch count: {tabSwitchCount}</p>
            </div>
          </div>
        </div>
      )}

      {showSubmitDialog && submitSummary && (
        <div className="fixed inset-0 z-[60] flex items-center justify-center bg-gray-950/55 px-4">
          <div className="w-full max-w-md rounded-3xl bg-white p-6 shadow-2xl">
            <h3 className="text-xl font-black text-gray-900">Submit test?</h3>
            <p className="mt-2 text-sm text-gray-600">
              You still have unanswered questions.
            </p>

            <div className="mt-5 grid grid-cols-3 gap-3 text-center">
              <div className="rounded-2xl bg-indigo-50 p-3">
                <p className="text-xs font-bold text-gray-500">Answered</p>
                <p className="text-xl font-black text-[#4F46E5]">{submitSummary.answeredCount}</p>
              </div>
              <div className="rounded-2xl bg-amber-50 p-3">
                <p className="text-xs font-bold text-gray-500">Unanswered</p>
                <p className="text-xl font-black text-amber-600">{submitSummary.unansweredCount}</p>
              </div>
              <div className="rounded-2xl bg-emerald-50 p-3">
                <p className="text-xs font-bold text-gray-500">Marked</p>
                <p className="text-xl font-black text-emerald-600">{submitSummary.reviewCount}</p>
              </div>
            </div>

            <div className="mt-6 flex items-center justify-end gap-3">
              <button
                onClick={() => setShowSubmitDialog(false)}
                className="rounded-xl border border-gray-200 px-4 py-2.5 text-sm font-bold text-gray-700 hover:bg-gray-50"
              >
                Go Back
              </button>
              <button
                onClick={() => void submitTest(true)}
                className="rounded-xl bg-[#10B981] px-4 py-2.5 text-sm font-bold text-white hover:bg-[#059669]"
              >
                Submit Anyway
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
};

export default TestTakingPage;
