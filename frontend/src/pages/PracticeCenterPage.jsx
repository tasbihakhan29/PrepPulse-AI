import React, { useState, useEffect, useRef } from 'react';
import { useLocation, useNavigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
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
  Upload,
  FileText,
  Trash2,
  ChevronRight,
  ChevronLeft,
  Sparkles,
  Lock,
  ArrowRight,
  CheckCircle2,
  AlertCircle,
  HelpCircle,
  Clock,
  RotateCcw,
  BookOpenCheck,
  ChevronDown,
  Info,
} from 'lucide-react';

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080';

const PracticeCenterPage = () => {
  const { user, logout } = useAuth();
  const navigate = useNavigate();
  const location = useLocation();
  
  const [activeTab, setActiveTab] = useState('AI Practice Center');
  const [sidebarOpen, setSidebarOpen] = useState(false);
  
  // Wizard state: 1: Material, 2: Exam, 3: Config, 4: Stream, 5: Quiz/Output
  const [step, setStep] = useState(1);
  
  // Step 1 State: Material (PDF vs Paste Text)
  const [materialType, setMaterialType] = useState('pdf'); // 'pdf' or 'text'
  const [pdfFile, setPdfFile] = useState(null);
  const [uploadProgress, setUploadProgress] = useState(0);
  const [uploading, setUploading] = useState(false);
  const [textContent, setTextContent] = useState('');
  const [materialId, setMaterialId] = useState(null);
  
  // Step 2 State: Exam Context
  const [examType, setExamType] = useState('GATE');
  const [availableTopics, setAvailableTopics] = useState([]);
  
  // Step 3 State: Test Configuration
  const [questionType, setQuestionType] = useState('Mixed');
  const [difficulty, setDifficulty] = useState('Mixed');
  const [questionCount, setQuestionCount] = useState(5);
  const [markingType, setMarkingType] = useState('predefined'); // 'predefined' or 'custom'
  const [predefinedMarking, setPredefinedMarking] = useState('+1 / 0');
  const [customMarking, setCustomMarking] = useState('+3 / -0.5');
  const [topicFocus, setTopicFocus] = useState('');

  // Step 4 State: Streaming AI Output
  const [streamStatus, setStreamStatus] = useState('Connecting to generation engine...');
  const [streamingText, setStreamingText] = useState('');
  const [generatedTest, setGeneratedTest] = useState(null);
  const streamAbortController = useRef(null);

  // Step 5 State: Interactive Testing
  const [testMode, setTestMode] = useState('review'); // 'review' (list questions), 'quiz' (taking exam), 'results' (quiz score)
  const [userAnswers, setUserAnswers] = useState({}); // {questionId: selectedOption}
  const [quizResults, setQuizResults] = useState(null); // {score, correct, total, topicScores}
  const [quizStartTime, setQuizStartTime] = useState(null);
  const [elapsedTime, setElapsedTime] = useState(0);
  const timerRef = useRef(null);
  
  // Textarea resize ref
  const textareaRef = useRef(null);

  useEffect(() => {
    if (!user) return;
  }, [user]);

  useEffect(() => {
    const presetFilters = location.state?.presetFilters;
    if (!presetFilters) {
      return;
    }

    if (presetFilters.examType) {
      setExamType(presetFilters.examType);
    }
    if (presetFilters.questionType) {
      setQuestionType(presetFilters.questionType);
    }
    if (presetFilters.difficulty) {
      setDifficulty(presetFilters.difficulty);
    }
    if (presetFilters.topicFocus) {
      setTopicFocus(presetFilters.topicFocus);
    }
    navigate(location.pathname, { replace: true, state: {} });
  }, [location.pathname, location.state, navigate]);

  // Handle word/char counter for pasted notes
  const getWordCount = (str) => {
    if (!str || str.trim() === '') return 0;
    return str.trim().split(/\s+/).length;
  };

  const handleTextChange = (e) => {
    setTextContent(e.target.value);
    // Auto-resize textarea
    if (textareaRef.current) {
      textareaRef.current.style.height = 'auto';
      textareaRef.current.style.height = `${textareaRef.current.scrollHeight}px`;
    }
  };

  // Drag & Drop PDF Handlers
  const handleDragOver = (e) => {
    e.preventDefault();
  };

  const handleDrop = (e) => {
    e.preventDefault();
    const files = e.dataTransfer.files;
    if (files && files.length > 0) {
      validateAndProcessPdf(files[0]);
    }
  };

  const handleFileChange = (e) => {
    const files = e.target.files;
    if (files && files.length > 0) {
      validateAndProcessPdf(files[0]);
    }
  };

  const validateAndProcessPdf = (file) => {
    if (file.type !== 'application/pdf') {
      toast.error('Only PDF documents are allowed.');
      return;
    }
    if (file.size > 5 * 1024 * 1024) {
      toast.error('File size must not exceed 5 MB.');
      return;
    }
    setPdfFile(file);
    simulateUpload(file);
  };

  const simulateUpload = (file) => {
    setUploading(true);
    setUploadProgress(0);
    const interval = setInterval(() => {
      setUploadProgress((prev) => {
        if (prev >= 100) {
          clearInterval(interval);
          setUploading(false);
          toast.success(`${file.name} validated successfully.`);
          return 100;
        }
        return prev + 10;
      });
    }, 80);
  };

  const removeFile = () => {
    setPdfFile(null);
    setUploadProgress(0);
  };

  // STEP 1 TRIGGER — Handle PDF or Notes Upload to Backend
  const handleStep1Submit = async () => {
    const formData = new FormData();
    if (materialType === 'pdf') {
      if (!pdfFile) {
        toast.error('Please drag & drop or browse a PDF study file.');
        return;
      }
      formData.append('file', pdfFile);
    } else {
      if (!textContent || textContent.trim().length < 30) {
        toast.error('Pasted study notes must be at least 30 characters.');
        return;
      }
      formData.append('textContent', textContent);
    }

    setUploading(true);
    try {
      const response = await fetch(`${API_BASE_URL}/api/practice/upload`, {
        method: 'POST',
        headers: {
          'Authorization': `Bearer ${localStorage.getItem('token')}`
        },
        body: formData
      });

      const data = await response.json();
      if (!response.ok) {
        throw new Error(data.message || 'Upload failed.');
      }

      setMaterialId(data.id);
      setAvailableTopics(data.topics || []);
      setTopicFocus('');
      toast.success(`Academic check passed (Confidence Score: ${data.academicConfidenceScore}%)`);
      setStep(2);
    } catch (err) {
      console.error(err);
      toast.error(err.message || 'Verification failed. Check your file.');
    } finally {
      setUploading(false);
    }
  };

  // SSE STREAM GENERATION
  const startStreamGeneration = async () => {
    setStep(4);
    setStreamingText('');
    setStreamStatus('Connecting to AI generation engine...');
    
    try {
      // 1. POST Generation config
      const configResponse = await fetch(`${API_BASE_URL}/api/practice/generate`, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          'Authorization': `Bearer ${localStorage.getItem('token')}`
        },
        body: JSON.stringify({
          sourceMaterialId: materialId,
          examType: examType,
          questionType: questionType,
          difficulty: difficulty,
          questionCount: parseInt(questionCount),
          markingScheme: markingType === 'predefined' ? predefinedMarking : customMarking,
          topicFocus: topicFocus
        })
      });

      const configData = await configResponse.json();
      if (!configResponse.ok) {
        throw new Error(configData.message || 'Failed to initialize generation config.');
      }

      const generationId = configData.generationId;

      // 2. Open Stream connection
      streamAbortController.current = new AbortController();
      const response = await fetch(`${API_BASE_URL}/api/practice/stream?generationId=${generationId}`, {
        headers: {
          'Authorization': `Bearer ${localStorage.getItem('token')}`
        },
        signal: streamAbortController.current.signal
      });

      if (!response.ok) {
        throw new Error('Connection to question stream failed.');
      }

      const reader = response.body.getReader();
      const decoder = new TextDecoder();
      let buffer = '';

      setStreamStatus('Streaming generated questions...');

      while (true) {
        const { done, value } = await reader.read();
        if (done) break;

        buffer += decoder.decode(value, { stream: true });
        
        // Parse SSE blocks split by double newline
        const blocks = buffer.split('\n\n');
        buffer = blocks.pop(); // Hold onto the last incomplete block

        for (const block of blocks) {
          const lines = block.split('\n');
          let eventType = '';
          let dataContent = '';

          for (const line of lines) {
            if (line.startsWith('event:')) {
              eventType = line.substring(6).trim();
            } else if (line.startsWith('data:')) {
              dataContent = line.substring(5).trim();
            }
          }

          if (eventType === 'info') {
            setStreamStatus(dataContent);
          } else if (eventType === 'token') {
            setStreamingText((prev) => prev + dataContent);
          } else if (eventType === 'complete') {
            const finalTest = JSON.parse(dataContent);
            setGeneratedTest(finalTest);
            navigate(`/test/${finalTest.id}`);
            toast.success('Practice quiz generated successfully!');
            return;
          } else if (eventType === 'error') {
            throw new Error(dataContent);
          }
        }
      }

    } catch (err) {
      if (err.name === 'AbortError') {
        toast.error('Generation cancelled by user.');
        setStep(3);
      } else {
        console.error(err);
        toast.error(err.message || 'An error occurred during streaming.');
        setStep(3);
      }
    }
  };

  const cancelGeneration = () => {
    if (streamAbortController.current) {
      streamAbortController.current.abort();
    }
  };

  // INTERACTIVE EXAM TAKING METHODS
  const startQuiz = () => {
    setUserAnswers({});
    setQuizResults(null);
    setTestMode('quiz');
    setQuizStartTime(Date.now());
    setElapsedTime(0);

    timerRef.current = setInterval(() => {
      setElapsedTime((prev) => prev + 1);
    }, 1000);
  };

  const selectAnswer = (questionId, option) => {
    setUserAnswers((prev) => ({
      ...prev,
      [questionId]: option
    }));
  };

  const submitQuizAnswers = async () => {
    clearInterval(timerRef.current);
    
    // Evaluate scores locally
    let correct = 0;
    const total = generatedTest.questions.length;
    
    // Track topic scoring: {topicName: {correct: 0, total: 0}}
    const topicTracker = {};

    generatedTest.questions.forEach((q) => {
      const isCorrect = userAnswers[q.id] === q.correctAnswer;
      if (isCorrect) correct++;

      const topic = q.topic || 'General';
      if (!topicTracker[topic]) {
        topicTracker[topic] = { correct: 0, total: 0 };
      }
      topicTracker[topic].total += 1;
      if (isCorrect) {
        topicTracker[topic].correct += 1;
      }
    });

    // Compute percentage scores per topic
    const topicScores = {};
    Object.keys(topicTracker).forEach((topic) => {
      const info = topicTracker[topic];
      topicScores[topic] = parseFloat(((info.correct / info.total) * 100).toFixed(1));
    });

    const percentageScore = parseFloat(((correct / total) * 100).toFixed(1));

    try {
      const res = await fetch(`${API_BASE_URL}/api/practice/submit-results`, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          'Authorization': `Bearer ${localStorage.getItem('token')}`
        },
        body: JSON.stringify({
          testId: generatedTest.id,
          score: percentageScore,
          correctQuestions: correct,
          totalQuestions: total,
          topicScores: topicScores
        })
      });

      if (!res.ok) throw new Error('Failed to submit results.');

      setQuizResults({
        score: percentageScore,
        correct,
        total,
        topicScores
      });
      setTestMode('results');
      toast.success('Test results uploaded securely!');
    } catch (err) {
      console.error(err);
      toast.error('Could not save your test stats.');
    }
  };

  const formatTime = (secs) => {
    const mins = Math.floor(secs / 60);
    const remainingSecs = secs % 60;
    return `${mins.toString().padStart(2, '0')}:${remainingSecs.toString().padStart(2, '0')}`;
  };

  const handleLogout = () => {
    logout();
    navigate('/login');
  };

  // Sidebar navigation mapping
  const navigationItems = [
    { name: 'Dashboard', icon: LayoutDashboard },
    { name: 'AI Practice Center', icon: BookOpen },
    { name: 'AI Answer Evaluator', icon: Award },
    { name: 'History & Analytics', icon: History },
  ];

  const handleNavigationClick = (itemName) => {
    if (itemName === 'Dashboard') {
      navigate('/dashboard');
    } else if (itemName === 'AI Answer Evaluator') {
      navigate('/answer-evaluator');
    } else if (itemName === 'History & Analytics') {
      navigate('/history-analytics');
    } else {
      setActiveTab(itemName);
      setSidebarOpen(false);
    }
  };

  const handleStartTest = (testId) => {
    navigate(`/test/${testId}`);
  };

  return (
    <div className="min-h-screen bg-[#F9FAFB] flex font-sans antialiased">
      
      {/* 1. DESKTOP SIDEBAR */}
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
                      : 'text-gray-500 hover:bg-gray-50 hover:text-gray-955'
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

          <div className="flex items-center gap-2">
            {user.profilePicture ? (
              <img
                src={user.profilePicture}
                alt={user.name}
                className="w-9 h-9 rounded-full border-2 border-indigo-150 object-cover shadow-sm"
                referrerPolicy="no-referrer"
              />
            ) : (
              <div className="w-9 h-9 rounded-full bg-[#4F46E5] flex items-center justify-center text-white font-extrabold text-xs shadow-sm">
                {user.name.charAt(0).toUpperCase()}
              </div>
            )}
          </div>
        </header>

        {/* WORKSPACE AREA */}
        <main className="flex-1 p-6 md:p-8 bg-[#F9FAFB] flex flex-col justify-start">
          
          {activeTab === 'AI Practice Center' ? (
            <div className="max-w-4xl w-full mx-auto space-y-8">
              
              {/* WIZARD PROCESS INDICATORS */}
              {step < 5 && (
                <div className="flex items-center justify-between bg-white px-6 py-4 rounded-2xl border border-gray-200/80 shadow-sm overflow-x-auto gap-4 select-none">
                  {[
                    { val: 1, label: 'Study Material' },
                    { val: 2, label: 'Exam Context' },
                    { val: 3, label: 'Configuration' },
                    { val: 4, label: 'AI Stream' }
                  ].map((s) => (
                    <div key={s.val} className="flex items-center gap-2 flex-shrink-0">
                      <div className={`w-8 h-8 rounded-full flex items-center justify-center text-xs font-black border-2 transition-colors ${
                        step === s.val
                          ? 'border-[#4F46E5] bg-indigo-50 text-[#4F46E5]'
                          : step > s.val
                          ? 'border-[#10B981] bg-[#10B981] text-white'
                          : 'border-gray-250 text-gray-400'
                      }`}>
                        {step > s.val ? '✓' : s.val}
                      </div>
                      <span className={`text-xs font-bold ${
                        step === s.val ? 'text-gray-900 font-extrabold' : 'text-gray-450'
                      }`}>
                        {s.label}
                      </span>
                      {s.val < 4 && <ChevronRight className="w-4 h-4 text-gray-300 ml-2" />}
                    </div>
                  ))}
                </div>
              )}

              {/* STEP 1: STUDY MATERIAL */}
              {step === 1 && (
                <div className="bg-white rounded-3xl border border-gray-200/80 shadow-sm overflow-hidden flex flex-col">
                  <div className="p-6 sm:p-8 border-b border-gray-100">
                    <h2 className="text-xl font-extrabold text-gray-900 mb-1">Select Study Source</h2>
                    <p className="text-xs text-gray-400 font-semibold">Provide your syllabus notes or textbook to validate academic depth.</p>
                    
                    {/* TABS SELECTOR */}
                    <div className="flex gap-2 mt-6 p-1 bg-gray-50 border border-gray-150 rounded-2xl w-fit">
                      <button
                        onClick={() => setMaterialType('pdf')}
                        className={`flex items-center gap-2 px-5 py-2.5 rounded-xl text-xs font-black transition-all ${
                          materialType === 'pdf' ? 'bg-white text-gray-900 shadow-sm border border-gray-150' : 'text-gray-450 hover:text-gray-700'
                        }`}
                      >
                        <Upload className="w-3.5 h-3.5" />
                        <span>Upload PDF</span>
                      </button>
                      <button
                        onClick={() => setMaterialType('text')}
                        className={`flex items-center gap-2 px-5 py-2.5 rounded-xl text-xs font-black transition-all ${
                          materialType === 'text' ? 'bg-white text-gray-900 shadow-sm border border-gray-150' : 'text-gray-450 hover:text-gray-700'
                        }`}
                      >
                        <FileText className="w-3.5 h-3.5" />
                        <span>Paste Notes</span>
                      </button>
                    </div>
                  </div>

                  <div className="p-6 sm:p-8 flex-1">
                    {materialType === 'pdf' ? (
                      /* PDF TAB */
                      <div className="space-y-6">
                        {!pdfFile ? (
                          <div
                            onDragOver={handleDragOver}
                            onDrop={handleDrop}
                            className="border-2 border-dashed border-gray-250/70 rounded-2xl p-10 flex flex-col items-center justify-center text-center cursor-pointer hover:border-[#4F46E5] hover:bg-indigo-50/10 transition-all group"
                          >
                            <div className="w-14 h-14 bg-indigo-50 border border-indigo-100/35 rounded-2xl flex items-center justify-center text-[#4F46E5] mb-4 group-hover:scale-105 transition-transform">
                              <Upload className="w-6 h-6" />
                            </div>
                            <label className="text-sm font-extrabold text-gray-800 cursor-pointer hover:underline mb-1">
                              Drag & drop PDF here, or <span className="text-[#4F46E5]">browse files</span>
                              <input type="file" accept=".pdf" onChange={handleFileChange} className="hidden" />
                            </label>
                            <span className="text-[10px] text-gray-400 font-bold uppercase">PDF ONLY &bull; MAX 5 MB</span>
                          </div>
                        ) : (
                          /* FILE PREVIEW */
                          <div className="border border-gray-200/80 rounded-2xl p-5 flex items-center justify-between bg-white shadow-sm">
                            <div className="flex items-center gap-3">
                              <div className="w-11 h-11 bg-rose-50 border border-rose-100 rounded-xl flex items-center justify-center text-[#EF4444]">
                                <FileText className="w-5 h-5" />
                              </div>
                              <div>
                                <h4 className="text-sm font-extrabold text-gray-800 break-all">{pdfFile.name}</h4>
                                <p className="text-xs text-gray-450 font-semibold">{(pdfFile.size / (1024 * 1024)).toFixed(2)} MB</p>
                              </div>
                            </div>
                            <button
                              onClick={removeFile}
                              className="p-2.5 text-gray-400 hover:bg-red-50 hover:text-[#EF4444] rounded-xl border border-transparent hover:border-red-100 transition-all"
                            >
                              <Trash2 className="w-4.5 h-4.5" />
                            </button>
                          </div>
                        )}

                        {/* UPLOAD PROGRESS BAR */}
                        {uploading && (
                          <div className="space-y-1.5">
                            <div className="flex justify-between text-xs font-bold text-gray-600">
                              <span>Checking academic structure...</span>
                              <span>{uploadProgress}%</span>
                            </div>
                            <div className="w-full h-2 bg-gray-100 rounded-full overflow-hidden border border-gray-200/40">
                              <div className="h-full bg-[#4F46E5] transition-all duration-150" style={{ width: `${uploadProgress}%` }} />
                            </div>
                          </div>
                        )}
                      </div>
                    ) : (
                      /* PASTE NOTES TAB */
                      <div className="space-y-4">
                        <textarea
                          ref={textareaRef}
                          value={textContent}
                          onChange={handleTextChange}
                          rows={6}
                          placeholder="Paste your syllabus notes, textbook chapters, or reference points here (minimum 30 characters)..."
                          className="w-full p-4 border border-gray-200 rounded-2xl bg-white shadow-sm placeholder-gray-400 text-gray-800 text-sm font-medium focus:ring-4 focus:ring-[#4F46E5]/10 focus:border-[#4F46E5] outline-none transition-all duration-200 resize-none min-h-[150px] leading-relaxed"
                        />
                        <div className="flex justify-between text-[10px] font-black text-gray-400 uppercase tracking-wider px-1">
                          <span>Characters: {textContent.length}</span>
                          <span>Words: {getWordCount(textContent)}</span>
                        </div>
                      </div>
                    )}
                  </div>

                  <div className="p-6 sm:p-8 bg-gray-50/50 border-t border-gray-100 flex justify-end">
                    <button
                      onClick={handleStep1Submit}
                      disabled={uploading}
                      className="inline-flex items-center justify-center px-6 py-3.5 bg-[#4F46E5] hover:bg-[#4338CA] text-white text-sm font-black rounded-2xl shadow-xl shadow-indigo-100/50 active:scale-[0.98] transition-all duration-150"
                    >
                      {uploading ? 'Processing File...' : 'Verify Academic Quality'}
                      <ChevronRight className="ml-2 w-4.5 h-4.5" />
                    </button>
                  </div>
                </div>
              )}

              {/* STEP 2: EXAM CONTEXT */}
              {step === 2 && (
                <div className="bg-white rounded-3xl border border-gray-200/80 shadow-sm overflow-hidden">
                  <div className="p-6 sm:p-8 border-b border-gray-100">
                    <h2 className="text-xl font-extrabold text-gray-900 mb-1">Select Exam Context</h2>
                    <p className="text-xs text-gray-400 font-semibold">This helps the AI align question formats to actual board standards.</p>
                  </div>

                  <div className="p-6 sm:p-8 space-y-6">
                    <div className="grid grid-cols-1 sm:grid-cols-2 md:grid-cols-3 gap-4">
                      {['GATE', 'UPSC', 'University Exam', 'SSC', 'Banking'].map((exam) => (
                        <button
                          key={exam}
                          onClick={() => setExamType(exam)}
                          className={`p-5 rounded-2xl border text-left flex flex-col justify-between h-28 group transition-all duration-200 ${
                            examType === exam
                              ? 'border-[#4F46E5] bg-indigo-50/20 shadow-sm'
                              : 'border-gray-200 hover:border-gray-300 hover:bg-gray-50/50'
                          }`}
                        >
                          <span className={`text-xs font-black uppercase tracking-wider ${
                            examType === exam ? 'text-[#4F46E5]' : 'text-gray-400'
                          }`}>
                            Predefined Exam
                          </span>
                          <span className="text-sm font-black text-gray-900 tracking-tight mt-auto block">
                            {exam}
                          </span>
                        </button>
                      ))}
                    </div>

                  </div>

                  <div className="p-6 sm:p-8 bg-gray-50/50 border-t border-gray-100 flex justify-between">
                    <button
                      onClick={() => setStep(1)}
                      className="inline-flex items-center justify-center px-6 py-3.5 border border-gray-200 bg-white text-gray-600 hover:bg-gray-50 active:scale-[0.98] text-sm font-bold rounded-2xl transition-all duration-150"
                    >
                      <ChevronLeft className="mr-2 w-4.5 h-4.5" />
                      Back
                    </button>
                    <button
                      onClick={() => {
                        setStep(3);
                      }}
                      className="inline-flex items-center justify-center px-6 py-3.5 bg-[#4F46E5] hover:bg-[#4338CA] text-white text-sm font-black rounded-2xl shadow-xl shadow-indigo-100/50 active:scale-[0.98] transition-all duration-150"
                    >
                      Configure Test
                      <ChevronRight className="ml-2 w-4.5 h-4.5" />
                    </button>
                  </div>
                </div>
              )}

              {/* STEP 3: TEST CONFIGURATION */}
              {step === 3 && (
                <div className="bg-white rounded-3xl border border-gray-200/80 shadow-sm overflow-hidden">
                  <div className="p-6 sm:p-8 border-b border-gray-100">
                    <h2 className="text-xl font-extrabold text-gray-900 mb-1">Customize Your Test</h2>
                    <p className="text-xs text-gray-400 font-semibold">Tweak structural options for generation output matching.</p>
                  </div>

                  <div className="p-6 sm:p-8 space-y-6">
                    <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
                      
                      {/* QUESTION TYPE */}
                      <div className="space-y-2.5">
                        <label className="block text-[11px] font-black text-gray-800 uppercase tracking-wider ml-1">
                          Question Format
                        </label>
                        <div className="grid grid-cols-2 gap-2">
                          {['MCQ', 'MSQ', 'Numerical', 'Mixed'].map((t) => (
                            <button
                              key={t}
                              type="button"
                              onClick={() => setQuestionType(t)}
                              className={`py-3 rounded-xl text-xs font-black text-center border transition-all ${
                                questionType === t
                                  ? 'border-[#4F46E5] bg-indigo-50/20 text-[#4F46E5]'
                                  : 'border-gray-200 text-gray-500 hover:bg-gray-50'
                              }`}
                            >
                              {t}
                            </button>
                          ))}
                        </div>
                      </div>

                      {/* DIFFICULTY */}
                      <div className="space-y-2.5">
                        <label className="block text-[11px] font-black text-gray-800 uppercase tracking-wider ml-1">
                          Difficulty Level
                        </label>
                        <div className="grid grid-cols-2 gap-2">
                          {['Easy', 'Medium', 'Hard', 'Mixed'].map((d) => (
                            <button
                              key={d}
                              type="button"
                              onClick={() => setDifficulty(d)}
                              className={`py-3 rounded-xl text-xs font-black text-center border transition-all ${
                                difficulty === d
                                  ? 'border-[#4F46E5] bg-indigo-50/20 text-[#4F46E5]'
                                  : 'border-gray-200 text-gray-500 hover:bg-gray-50'
                              }`}
                            >
                              {d}
                            </button>
                          ))}
                        </div>
                      </div>

                      {/* QUESTION COUNT */}
                      <div className="space-y-2.5">
                        <label className="block text-[11px] font-black text-gray-800 uppercase tracking-wider ml-1">
                          Question Count
                        </label>
                        <div className="relative">
                          <select
                            value={questionCount}
                            onChange={(e) => setQuestionCount(e.target.value)}
                            className="block w-full px-4 py-3.5 border border-gray-200 rounded-2xl bg-white shadow-sm text-gray-800 text-sm font-semibold outline-none focus:ring-4 focus:ring-indigo-500/10 focus:border-[#4F46E5] appearance-none"
                          >
                            {[5, 10, 15, 20, 25, 30].map((num) => (
                              <option key={num} value={num}>{num} Questions</option>
                            ))}
                          </select>
                          <ChevronDown className="w-4.5 h-4.5 absolute right-4 top-1/2 -translate-y-1/2 text-gray-400 pointer-events-none" />
                        </div>
                      </div>

                      {/* TOPIC FOCUS (OPTIONAL) */}
                      <div className="space-y-2.5">
                        <label className="block text-[11px] font-black text-gray-800 uppercase tracking-wider ml-1">
                          Topic Focus (Optional)
                        </label>
                        <div className="relative">
                          <select
                            value={topicFocus}
                            onChange={(e) => setTopicFocus(e.target.value)}
                            className="block w-full px-4 py-3.5 border border-gray-200 rounded-2xl bg-white shadow-sm text-gray-800 text-sm font-semibold outline-none focus:ring-4 focus:ring-[#4F46E5]/10 focus:border-[#4F46E5] appearance-none"
                          >
                            <option value="">
                              {availableTopics.length > 0 ? 'Use all detected topics' : 'No specific topics detected'}
                            </option>
                            {availableTopics.map((topic) => (
                              <option key={topic} value={topic}>{topic}</option>
                            ))}
                          </select>
                          <ChevronDown className="w-4.5 h-4.5 absolute right-4 top-1/2 -translate-y-1/2 text-gray-400 pointer-events-none" />
                        </div>
                      </div>

                      {/* MARKING SCHEME */}
                      <div className="space-y-2.5 md:col-span-2 border-t border-gray-100 pt-6">
                        <label className="block text-[11px] font-black text-gray-800 uppercase tracking-wider ml-1 mb-2">
                          Marking Scheme Setup
                        </label>
                        <div className="flex flex-col sm:flex-row gap-4">
                          <div className="flex items-center gap-2">
                            <input
                              type="radio"
                              id="markingPredefined"
                              name="markingType"
                              checked={markingType === 'predefined'}
                              onChange={() => setMarkingType('predefined')}
                              className="w-4 h-4 text-[#4F46E5] border-gray-300 focus:ring-[#4F46E5]"
                            />
                            <label htmlFor="markingPredefined" className="text-sm font-semibold text-gray-700">Predefined Template</label>
                          </div>
                          <div className="flex items-center gap-2">
                            <input
                              type="radio"
                              id="markingCustom"
                              name="markingType"
                              checked={markingType === 'custom'}
                              onChange={() => setMarkingType('custom')}
                              className="w-4 h-4 text-[#4F46E5] border-gray-300 focus:ring-[#4F46E5]"
                            />
                            <label htmlFor="markingCustom" className="text-sm font-semibold text-gray-700">Custom Ratio</label>
                          </div>
                        </div>

                        {markingType === 'predefined' ? (
                          <div className="grid grid-cols-3 gap-2 mt-3 animate-fadeIn">
                            {['+1 / 0', '+2 / -0.66', '+4 / -1'].map((scheme) => (
                              <button
                                key={scheme}
                                type="button"
                                onClick={() => setPredefinedMarking(scheme)}
                                className={`py-3 rounded-xl text-xs font-black border text-center transition-all ${
                                  predefinedMarking === scheme
                                    ? 'border-[#4F46E5] bg-indigo-50/20 text-[#4F46E5]'
                                    : 'border-gray-200 text-gray-500 hover:bg-gray-50'
                                }`}
                              >
                                {scheme}
                              </button>
                            ))}
                          </div>
                        ) : (
                          <input
                            type="text"
                            value={customMarking}
                            onChange={(e) => setCustomMarking(e.target.value)}
                            placeholder="e.g. +3 / -0.5"
                            className="block w-full px-4 py-3.5 border border-gray-200 rounded-2xl bg-white shadow-sm mt-3 text-sm font-semibold focus:ring-4 focus:ring-indigo-500/10 focus:border-[#4F46E5] outline-none transition-all animate-fadeIn"
                          />
                        )}
                      </div>

                    </div>
                  </div>

                  <div className="p-6 sm:p-8 bg-gray-50/50 border-t border-gray-100 flex justify-between">
                    <button
                      onClick={() => setStep(2)}
                      className="inline-flex items-center justify-center px-6 py-3.5 border border-gray-200 bg-white text-gray-600 hover:bg-gray-50 active:scale-[0.98] text-sm font-bold rounded-2xl transition-all duration-150"
                    >
                      <ChevronLeft className="mr-2 w-4.5 h-4.5" />
                      Back
                    </button>
                    <button
                      onClick={startStreamGeneration}
                      className="inline-flex items-center justify-center px-6 py-3.5 bg-[#4F46E5] hover:bg-[#4338CA] text-white text-sm font-black rounded-2xl shadow-xl shadow-indigo-100/50 active:scale-[0.98] transition-all duration-150"
                    >
                      <Sparkles className="mr-2 w-4.5 h-4.5 text-[#10B981]" />
                      Generate Practice Test
                    </button>
                  </div>
                </div>
              )}

              {/* STEP 4: STREAM GENERATION PROGRESS */}
              {step === 4 && (
                <div className="bg-white rounded-3xl border border-gray-200/80 shadow-sm p-8 flex flex-col items-center justify-center min-h-[400px]">
                  <div className="w-16 h-16 bg-indigo-50 border border-indigo-100/35 rounded-2xl flex items-center justify-center text-[#4F46E5] mb-6 animate-pulse">
                    <Sparkles className="w-8 h-8 text-[#10B981]" />
                  </div>
                  
                  <h3 className="text-lg font-black text-gray-900 mb-2">Generating Questions</h3>
                  <p className="text-sm text-gray-450 font-semibold mb-6 flex items-center gap-2">
                    <span className="w-2.5 h-2.5 rounded-full bg-[#10B981] animate-ping flex-shrink-0" />
                    {streamStatus}
                  </p>

                  {/* STREAM RAW TYPEWRITER EFFECT */}
                  <div className="w-full max-w-lg bg-gray-950 text-emerald-400 font-mono text-xs p-5 rounded-2xl h-44 overflow-y-auto leading-relaxed border border-gray-800 shadow-inner select-none mb-8">
                    {streamingText ? (
                      <div className="whitespace-pre-wrap">{streamingText}</div>
                    ) : (
                      <div className="text-gray-650 animate-pulse">&gt; Initializing connection to Groq Llama 3.1 model...</div>
                    )}
                  </div>

                  <button
                    onClick={cancelGeneration}
                    className="inline-flex items-center justify-center px-5 py-3 border border-red-200 bg-red-50/40 text-[#EF4444] hover:bg-red-50 hover:border-red-300 active:scale-[0.98] text-sm font-bold rounded-xl transition-all duration-150"
                  >
                    Cancel Generation
                  </button>
                </div>
              )}

              {/* STEP 5: OUTPUT ACTIONS AND INTERACTIVE TESTING */}
              {step === 5 && generatedTest && (
                <div className="space-y-6">
                  
                  {/* GENERATION SUMMARY & TABS */}
                  <div className="bg-white rounded-3xl border border-gray-200/80 shadow-sm p-6 flex flex-col sm:flex-row items-center justify-between gap-4">
                    <div>
                      <div className="flex flex-wrap items-center gap-2 mb-1.5">
                        <h2 className="text-lg font-black text-gray-900 tracking-tight">Quiz Complete</h2>
                        <span className="px-2 py-0.5 bg-indigo-50 text-indigo-700 border border-indigo-100/50 rounded-lg text-[10px] font-black uppercase">
                          {generatedTest.examType}
                        </span>
                        <span className="px-2 py-0.5 bg-gray-50 text-gray-600 border border-gray-150 rounded-lg text-[10px] font-black uppercase">
                          {generatedTest.difficulty}
                        </span>
                      </div>
                      <p className="text-xs text-gray-450 font-semibold">
                        Contains {generatedTest.totalQuestions} questions. Choose an option below to study.
                      </p>
                    </div>

                    <div className="flex flex-wrap gap-2 w-full sm:w-auto">
                      <button
                        onClick={() => { setTestMode('review'); }}
                        className={`flex-1 sm:flex-initial px-4 py-2.5 rounded-xl text-xs font-black transition-all border ${
                          testMode === 'review' ? 'bg-indigo-50 border-indigo-150 text-[#4F46E5]' : 'bg-white border-gray-200 text-gray-500 hover:bg-gray-50'
                        }`}
                      >
                        Review Mode
                      </button>
                      <button
                        onClick={startQuiz}
                        className={`flex-1 sm:flex-initial px-4 py-2.5 rounded-xl text-xs font-black transition-all border ${
                          testMode === 'quiz' || testMode === 'results' ? 'bg-indigo-50 border-indigo-150 text-[#4F46E5]' : 'bg-white border-gray-200 text-gray-500 hover:bg-gray-50'
                        }`}
                      >
                        Start Test
                      </button>
                    </div>
                  </div>

                  {/* 5A. REVIEW MODE */}
                  {testMode === 'review' && (
                    <div className="space-y-4">
                      {generatedTest.questions.map((q, idx) => (
                        <div key={q.id} className="bg-white p-6 rounded-3xl border border-gray-200/80 shadow-sm space-y-4">
                          <div className="flex justify-between items-start gap-4">
                            <span className="w-8 h-8 rounded-xl bg-gray-50 border border-gray-150 flex items-center justify-center text-xs font-black text-gray-900 flex-shrink-0 mt-0.5">
                              {idx + 1}
                            </span>
                            <div className="flex-1 space-y-2">
                              <p className="text-sm font-bold text-gray-900 leading-relaxed">{q.question}</p>
                              
                              {/* BADGES */}
                              <div className="flex gap-2">
                                {q.topic && (
                                  <span className="inline-flex items-center px-2 py-0.5 rounded-lg text-[10px] font-bold bg-indigo-50/50 text-[#4F46E5] border border-indigo-100/30">
                                    {q.topic}
                                  </span>
                                )}
                                {q.difficulty && (
                                  <span className="inline-flex items-center px-2 py-0.5 rounded-lg text-[10px] font-bold bg-gray-50 text-gray-500 border border-gray-200/50">
                                    {q.difficulty}
                                  </span>
                                )}
                              </div>
                            </div>
                          </div>

                          {/* OPTIONS */}
                          <div className="grid grid-cols-1 sm:grid-cols-2 gap-3 pt-2">
                            {q.options && q.options.length > 0 ? (
                              q.options.map((opt, i) => (
                                <div
                                  key={i}
                                  className={`p-4 rounded-xl border text-xs font-bold leading-normal ${
                                    opt === q.correctAnswer
                                      ? 'border-[#10B981] bg-emerald-50/20 text-[#10B981]'
                                      : 'border-gray-150 bg-white text-gray-700'
                                  }`}
                                >
                                  {opt}
                                </div>
                              ))
                            ) : (
                              <div className="p-4 rounded-xl border border-emerald-150 bg-emerald-50/10 text-emerald-800 text-xs font-bold sm:col-span-2">
                                Correct Numerical Solution: {q.correctAnswer}
                              </div>
                            )}
                          </div>

                          {/* EXPLANATION */}
                          {q.explanation && (
                            <div className="mt-4 p-4 rounded-2xl bg-gray-50 border border-gray-150 text-xs font-semibold text-gray-600 leading-relaxed">
                              <span className="flex items-center gap-1.5 font-black text-gray-800 uppercase tracking-wider text-[10px] mb-1.5">
                                <Info className="w-3.5 h-3.5 text-indigo-500" />
                                Solution Explanation
                              </span>
                              {q.explanation}
                            </div>
                          )}
                        </div>
                      ))}
                      <div className="flex justify-between mt-8">
                        <button
                          onClick={() => setStep(3)}
                          className="inline-flex items-center justify-center px-6 py-3.5 border border-gray-200 bg-white text-gray-600 hover:bg-gray-50 active:scale-[0.98] text-sm font-bold rounded-2xl transition-all"
                        >
                          Generate New Test
                        </button>
                        <button
                          onClick={() => navigate('/dashboard')}
                          className="inline-flex items-center justify-center px-6 py-3.5 bg-[#4F46E5] text-white text-sm font-black rounded-2xl hover:bg-[#4338CA] active:scale-[0.98] transition-all"
                        >
                          Return to Workspace
                        </button>
                      </div>
                    </div>
                  )}

                  {/* 5B. QUIZ MODE */}
                  {testMode === 'quiz' && (
                    <div className="space-y-6">
                      
                      {/* QUIZ TIMER HEADER */}
                      <div className="bg-white p-4 rounded-2xl border border-gray-200/80 shadow-sm flex items-center justify-between sticky top-16 z-10">
                        <div className="flex items-center gap-2 text-gray-700 font-bold text-sm">
                          <Clock className="w-5 h-5 text-indigo-500" />
                          <span>Elapsed Duration: <span className="font-extrabold text-[#4F46E5] font-mono">{formatTime(elapsedTime)}</span></span>
                        </div>
                        <span className="text-xs font-bold text-gray-500">
                          Answered: {Object.keys(userAnswers).length} / {generatedTest.questions.length}
                        </span>
                      </div>

                      {/* QUIZ QUESTIONS */}
                      <div className="space-y-6">
                        {generatedTest.questions.map((q, idx) => (
                          <div key={q.id} className="bg-white p-6 rounded-3xl border border-gray-200/80 shadow-sm space-y-4">
                            <div className="flex gap-4">
                              <span className="w-8 h-8 rounded-xl bg-indigo-50 text-[#4F46E5] border border-indigo-100/30 flex items-center justify-center text-xs font-black flex-shrink-0">
                                {idx + 1}
                              </span>
                              <p className="text-sm font-bold text-gray-900 leading-relaxed pt-1">{q.question}</p>
                            </div>

                            {/* OPTION SELECTIONS */}
                            <div className="grid grid-cols-1 sm:grid-cols-2 gap-3 pt-2">
                              {q.options && q.options.length > 0 ? (
                                q.options.map((opt, i) => {
                                  const isSelected = userAnswers[q.id] === opt;
                                  return (
                                    <button
                                      key={i}
                                      onClick={() => selectAnswer(q.id, opt)}
                                      className={`p-4 rounded-xl border text-left text-xs font-bold transition-all leading-normal ${
                                        isSelected
                                          ? 'border-[#4F46E5] bg-indigo-50/20 text-[#4F46E5]'
                                          : 'border-gray-200 bg-white text-gray-700 hover:border-gray-300'
                                      }`}
                                    >
                                      {opt}
                                    </button>
                                  );
                                })
                              ) : (
                                <div className="sm:col-span-2">
                                  <input
                                    type="text"
                                    value={userAnswers[q.id] || ''}
                                    onChange={(e) => selectAnswer(q.id, e.target.value)}
                                    placeholder="Enter your numerical solution value..."
                                    className="block w-full px-4 py-3.5 border border-gray-200 rounded-2xl bg-white shadow-sm text-sm font-bold focus:ring-4 focus:ring-indigo-500/10 focus:border-[#4F46E5] outline-none transition-all"
                                  />
                                </div>
                              )}
                            </div>
                          </div>
                        ))}
                      </div>

                      {/* SUBMIT BUTTON ROW */}
                      <div className="flex justify-between border-t border-gray-250/50 pt-6">
                        <button
                          onClick={() => { setTestMode('review'); clearInterval(timerRef.current); }}
                          className="inline-flex items-center justify-center px-5 py-3.5 border border-gray-200 bg-white text-gray-500 hover:bg-gray-50 active:scale-[0.98] text-sm font-bold rounded-2xl transition-all"
                        >
                          Abort Exam
                        </button>
                        <button
                          onClick={submitQuizAnswers}
                          className="inline-flex items-center justify-center px-6 py-3.5 bg-[#10B981] hover:bg-[#0D9488] text-white text-sm font-black rounded-2xl shadow-xl shadow-emerald-100/50 active:scale-[0.98] transition-all"
                        >
                          Submit Test Answers
                          <CheckCircle2 className="ml-2 w-4.5 h-4.5" />
                        </button>
                      </div>

                    </div>
                  )}

                  {/* 5C. RESULTS DASHBOARD */}
                  {testMode === 'results' && quizResults && (
                    <div className="space-y-6">
                      
                      {/* STAT PANEL */}
                      <div className="bg-white rounded-3xl border border-gray-200/80 shadow-sm p-6 sm:p-8 flex flex-col md:flex-row items-center justify-around gap-6 select-none">
                        <div className="text-center">
                          <span className="block text-[10px] font-black text-gray-400 uppercase tracking-wider mb-1">Percentage Score</span>
                          <h2 className={`text-5xl font-black ${
                            quizResults.score >= 70 ? 'text-[#10B981]' : 'text-orange-550'
                          }`}>
                            {quizResults.score}%
                          </h2>
                        </div>
                        <div className="w-px h-16 bg-gray-200 hidden md:block" />
                        <div className="text-center">
                          <span className="block text-[10px] font-black text-gray-400 uppercase tracking-wider mb-1">Correct Questions</span>
                          <h3 className="text-3xl font-black text-gray-900">{quizResults.correct} / {quizResults.total}</h3>
                        </div>
                      </div>

                      {/* TOPIC SCORE PILLS */}
                      <div className="bg-white rounded-3xl border border-gray-200/80 shadow-sm p-6 space-y-4">
                        <h3 className="text-sm font-black text-gray-900 uppercase tracking-wider">Scoring Breakdown by Topic</h3>
                        <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
                          {Object.keys(quizResults.topicScores).map((topic) => {
                            const score = quizResults.topicScores[topic];
                            return (
                              <div key={topic} className="flex items-center justify-between p-4 bg-gray-50/50 border border-gray-150 rounded-2xl">
                                <span className="text-xs font-bold text-gray-800">{topic}</span>
                                <span className={`text-xs font-black ${
                                  score >= 70 ? 'text-[#10B981]' : 'text-orange-650'
                                }`}>
                                  {score}%
                                </span>
                              </div>
                            );
                          })}
                        </div>
                      </div>

                      {/* BACK NAVIGATION */}
                      <div className="flex justify-center gap-4">
                        <button
                          onClick={() => { setStep(3); }}
                          className="inline-flex items-center justify-center px-5 py-3.5 border border-gray-200 bg-white text-gray-600 hover:bg-gray-50 active:scale-[0.98] text-sm font-bold rounded-2xl transition-all"
                        >
                          <RotateCcw className="w-4 h-4 mr-2" />
                          New Test
                        </button>
                        <button
                          onClick={() => { navigate('/dashboard'); }}
                          className="inline-flex items-center justify-center px-6 py-3.5 bg-[#4F46E5] text-white text-sm font-black rounded-2xl hover:bg-[#4338CA] active:scale-[0.98] transition-all"
                        >
                          Return to Dashboard
                        </button>
                      </div>

                    </div>
                  )}

                </div>
              )}

            </div>
          ) : (
            /* COMING SOON CARD FOR EVALUATOR / HISTORY */
            <div className="max-w-xl mx-auto flex flex-col items-center justify-center min-h-[55vh] bg-white rounded-3xl border border-gray-200/80 p-8 sm:p-16 text-center shadow-sm">
              <div className="w-16 h-16 bg-indigo-50 border border-indigo-100/35 rounded-2xl flex items-center justify-center text-[#4F46E5] mb-6">
                <Lock className="w-6 h-6 animate-pulse" />
              </div>
              <h2 className="text-2xl font-black text-gray-900 tracking-tight mb-2">
                {activeTab} is Coming Soon
              </h2>
              <p className="text-gray-450 text-sm font-semibold max-w-sm mb-8 leading-relaxed">
                We are currently building this feature. It will be available in the next release!
              </p>
              <button
                onClick={() => handleNavigationClick('AI Practice Center')}
                className="inline-flex items-center justify-center px-5 py-3 bg-[#4F46E5] text-white text-sm font-black rounded-xl hover:bg-[#4338CA] active:scale-[0.98] transition-all shadow-md"
              >
                Return to Practice Center
              </button>
            </div>
          )}
        </main>
      </div>

      {/* Styled Inline Keyframes for 3D Flipping Effects */}
      <style dangerouslySetInnerHTML={{ __html: `
        .perspective {
          perspective: 1000px;
        }
        .transform-style {
          transform-style: preserve-3d;
        }
        .backface-hidden {
          backface-visibility: hidden;
        }
        .rotate-y-180 {
          transform: rotateY(180deg);
        }
        @keyframes fadeIn {
          from { opacity: 0; transform: translateY(5px); }
          to { opacity: 1; transform: translateY(0); }
        }
        .animate-fadeIn {
          animation: fadeIn 0.3s forwards ease-out;
        }
      `}} />

    </div>
  );
};

export default PracticeCenterPage;
