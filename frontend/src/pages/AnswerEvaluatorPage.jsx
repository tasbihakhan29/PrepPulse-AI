import React, { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import { evaluatorApi } from '../api/evaluator';
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
  CheckCircle2,
  AlertCircle,
  Clock,
  Search,
  Eye,
  XCircle,
  Loader2,
  FileImage,
  File,
} from 'lucide-react';

const AnswerEvaluatorPage = () => {
  const { user, logout } = useAuth();
  const navigate = useNavigate();

  const [sidebarOpen, setSidebarOpen] = useState(false);
  const [activeTab, setActiveTab] = useState('AI Answer Evaluator');

  // Form state
  const [question, setQuestion] = useState('');
  const [topic, setTopic] = useState('');
  const [marksLimit, setMarksLimit] = useState(7);
  const [customMarks, setCustomMarks] = useState('');
  const [file, setFile] = useState(null);
  const [uploadProgress, setUploadProgress] = useState(0);
  const [uploading, setUploading] = useState(false);

  // Evaluation state
  const [evaluating, setEvaluating] = useState(false);
  const [evaluationResult, setEvaluationResult] = useState(null);
  const [submissionId, setSubmissionId] = useState(null);

  // History state
  const [history, setHistory] = useState([]);
  const [loadingHistory, setLoadingHistory] = useState(false);
  const [searchQuery, setSearchQuery] = useState('');
  const [currentPage, setCurrentPage] = useState(0);
  const [totalPages, setTotalPages] = useState(0);

  // Modal state
  const [showDetailsModal, setShowDetailsModal] = useState(false);
  const [selectedEvaluation, setSelectedEvaluation] = useState(null);

  useEffect(() => {
    if (user) {
      fetchHistory();
    }
  }, [user, currentPage]);

  const fetchHistory = async () => {
    setLoadingHistory(true);
    try {
      const response = await evaluatorApi.getEvaluationHistory(searchQuery, currentPage, 10);
      setHistory(response.content);
      setTotalPages(response.totalPages);
    } catch (err) {
      console.error('Error fetching history:', err);
      toast.error('Failed to load evaluation history.');
    } finally {
      setLoadingHistory(false);
    }
  };

  const handleDragOver = (e) => {
    e.preventDefault();
  };

  const handleDrop = (e) => {
    e.preventDefault();
    const files = e.dataTransfer.files;
    if (files && files.length > 0) {
      validateAndProcessFile(files[0]);
    }
  };

  const handleFileChange = (e) => {
    const files = e.target.files;
    if (files && files.length > 0) {
      validateAndProcessFile(files[0]);
    }
  };

  const validateAndProcessFile = (file) => {
    const allowedTypes = ['image/jpeg', 'image/jpg', 'image/png', 'image/webp', 'application/pdf'];
    if (!allowedTypes.includes(file.type)) {
      toast.error('Only JPG, JPEG, PNG, WEBP, and PDF files are allowed.');
      return;
    }
    if (file.size > 10 * 1024 * 1024) {
      toast.error('File size must not exceed 10 MB.');
      return;
    }
    setFile(file);
    simulateUpload(file);
  };

  const simulateUpload = (file) => {
    setUploading(true);
    setUploadProgress(0);
    const interval = setInterval(() => {
      let completed = false;
      setUploadProgress((prev) => {
        const nextProgress = Math.min(prev + 10, 100);
        completed = nextProgress === 100;
        return nextProgress;
      });

      if (completed) {
        clearInterval(interval);
        setUploading(false);
        toast.success(`${file.name} validated successfully.`);
      }
    }, 80);
  };

  const removeFile = () => {
    setFile(null);
    setUploadProgress(0);
  };

  const handleUpload = async () => {
    if (!file) {
      toast.error('Please upload an answer file.');
      return;
    }
    if (!question.trim()) {
      toast.error('Please enter the question.');
      return;
    }
    if (marksLimit === 'custom' && (!customMarks || customMarks <= 0)) {
      toast.error('Please enter valid custom marks.');
      return;
    }

    const finalMarks = marksLimit === 'custom' ? parseFloat(customMarks) : marksLimit;

    setUploading(true);
    try {
      const response = await evaluatorApi.uploadAnswer(file, question, topic, finalMarks);
      setSubmissionId(response.submissionId);
      toast.success('Answer uploaded successfully!');
      setEvaluationResult(null);
    } catch (err) {
      console.error('Upload error:', err);
      toast.error(err.response?.data?.message || 'Upload failed.');
    } finally {
      setUploading(false);
    }
  };

  const handleEvaluate = async () => {
    if (!submissionId) {
      toast.error('Please upload an answer first.');
      return;
    }

    setEvaluating(true);
    try {
      const response = await evaluatorApi.evaluateAnswer(submissionId);
      setEvaluationResult(response);
      toast.success('Evaluation completed!');
      fetchHistory();
    } catch (err) {
      console.error('Evaluation error:', err);
      toast.error(err.response?.data?.message || 'Evaluation failed.');
    } finally {
      setEvaluating(false);
    }
  };

  const handleSearch = (e) => {
    setSearchQuery(e.target.value);
    setCurrentPage(0);
  };

  useEffect(() => {
    const timeoutId = setTimeout(() => {
      fetchHistory();
    }, 500);
    return () => clearTimeout(timeoutId);
  }, [searchQuery]);

  const handleViewDetails = async (evaluation) => {
    setSelectedEvaluation(evaluation);
    if (evaluation.evaluationId) {
      try {
        const details = await evaluatorApi.getEvaluationById(evaluation.evaluationId);
        setSelectedEvaluation({ ...evaluation, ...details });
      } catch (err) {
        console.error('Error fetching details:', err);
        toast.error('Failed to load evaluation details.');
      }
    }
    setShowDetailsModal(true);
  };

  const handleLogout = () => {
    logout();
    navigate('/login');
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

  const getFileIcon = (fileType) => {
    if (fileType?.includes('pdf')) return <File className="w-5 h-5" />;
    return <FileImage className="w-5 h-5" />;
  };

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

      {/* MOBILE SIDEBAR DRAWER */}
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

      {/* MAIN WORKSPACE CONTENT */}
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

        {/* WORKSPACE AREA */}
        <main className="flex-1 p-6 md:p-8 bg-[#F9FAFB] space-y-8 overflow-y-auto">
          
          <div className="max-w-6xl mx-auto space-y-8">
            
            {/* TWO COLUMN LAYOUT */}
            <div className="grid grid-cols-1 lg:grid-cols-2 gap-8">
              
              {/* LEFT COLUMN - UPLOAD FORM */}
              <div className="space-y-6">
                
                {/* SECTION 1: QUESTION DETAILS */}
                <div className="bg-white rounded-3xl border border-gray-200/80 shadow-sm overflow-hidden">
                  <div className="p-6 border-b border-gray-100">
                    <h2 className="text-lg font-extrabold text-gray-900 mb-1">Question Details</h2>
                    <p className="text-xs text-gray-400 font-semibold">Enter the question for evaluation</p>
                  </div>

                  <div className="p-6 space-y-5">
                    <div className="space-y-2.5">
                      <label className="block text-[11px] font-black text-gray-800 uppercase tracking-wider ml-1">
                        Question Text <span className="text-red-500">*</span>
                      </label>
                      <textarea
                        value={question}
                        onChange={(e) => setQuestion(e.target.value)}
                        rows={4}
                        placeholder="e.g., Explain TCP Three Way Handshake. What is Normalization?"
                        className="w-full p-4 border border-gray-200 rounded-2xl bg-white shadow-sm placeholder-gray-400 text-gray-800 text-sm font-medium focus:ring-4 focus:ring-[#4F46E5]/10 focus:border-[#4F46E5] outline-none transition-all duration-200 resize-none"
                      />
                    </div>

                    <div className="space-y-2.5">
                      <label className="block text-[11px] font-black text-gray-800 uppercase tracking-wider ml-1">
                        Topic (Optional)
                      </label>
                      <input
                        type="text"
                        value={topic}
                        onChange={(e) => setTopic(e.target.value)}
                        placeholder="e.g., DBMS, OS, CN, DSA"
                        className="block w-full px-4 py-3.5 border border-gray-200 rounded-2xl bg-white shadow-sm placeholder-gray-400 text-gray-800 text-sm font-medium focus:ring-4 focus:ring-[#4F46E5]/10 focus:border-[#4F46E5] outline-none transition-all duration-200"
                      />
                    </div>
                  </div>
                </div>

                {/* SECTION 2: MARKS CONFIGURATION */}
                <div className="bg-white rounded-3xl border border-gray-200/80 shadow-sm overflow-hidden">
                  <div className="p-6 border-b border-gray-100">
                    <h2 className="text-lg font-extrabold text-gray-900 mb-1">Marks Configuration</h2>
                    <p className="text-xs text-gray-400 font-semibold">Set maximum marks for evaluation</p>
                  </div>

                  <div className="p-6 space-y-5">
                    <div className="space-y-2.5">
                      <label className="block text-[11px] font-black text-gray-800 uppercase tracking-wider ml-1">
                        Maximum Marks
                      </label>
                      <div className="grid grid-cols-3 sm:grid-cols-5 gap-3">
                        {[2, 3, 5, 7, 10].map((marks) => (
                          <button
                            key={marks}
                            onClick={() => {
                              setMarksLimit(marks);
                              setCustomMarks('');
                            }}
                            className={`p-3 rounded-xl border text-center text-sm font-black transition-all ${
                              marksLimit === marks
                                ? 'border-[#4F46E5] bg-indigo-50 text-[#4F46E5]'
                                : 'border-gray-200 text-gray-600 hover:border-gray-300 hover:bg-gray-50'
                            }`}
                          >
                            {marks}
                          </button>
                        ))}
                        <button
                          onClick={() => setMarksLimit('custom')}
                          className={`p-3 rounded-xl border text-center text-sm font-black transition-all ${
                            marksLimit === 'custom'
                              ? 'border-[#4F46E5] bg-indigo-50 text-[#4F46E5]'
                              : 'border-gray-200 text-gray-600 hover:border-gray-300 hover:bg-gray-50'
                          }`}
                        >
                          Custom
                        </button>
                      </div>
                    </div>

                    {marksLimit === 'custom' && (
                      <div className="space-y-2.5 animate-fadeIn">
                        <label className="block text-[11px] font-black text-gray-800 uppercase tracking-wider ml-1">
                          Custom Marks
                        </label>
                        <input
                          type="number"
                          value={customMarks}
                          onChange={(e) => setCustomMarks(e.target.value)}
                          placeholder="Enter custom marks"
                          min="1"
                          className="block w-full px-4 py-3.5 border border-gray-200 rounded-2xl bg-white shadow-sm placeholder-gray-400 text-gray-800 text-sm font-medium focus:ring-4 focus:ring-[#4F46E5]/10 focus:border-[#4F46E5] outline-none transition-all duration-200"
                        />
                      </div>
                    )}
                  </div>
                </div>

                {/* SECTION 3: ANSWER UPLOAD */}
                <div className="bg-white rounded-3xl border border-gray-200/80 shadow-sm overflow-hidden">
                  <div className="p-6 border-b border-gray-100">
                    <h2 className="text-lg font-extrabold text-gray-900 mb-1">Upload Answer</h2>
                    <p className="text-xs text-gray-400 font-semibold">Upload handwritten answer, scanned PDF, or diagram</p>
                  </div>

                  <div className="p-6 space-y-5">
                    {!file ? (
                      <div
                        onDragOver={handleDragOver}
                        onDrop={handleDrop}
                        className="border-2 border-dashed border-gray-250/70 rounded-2xl p-10 flex flex-col items-center justify-center text-center cursor-pointer hover:border-[#4F46E5] hover:bg-indigo-50/10 transition-all group"
                      >
                        <div className="w-14 h-14 bg-indigo-50 border border-indigo-100/35 rounded-2xl flex items-center justify-center text-[#4F46E5] mb-4 group-hover:scale-105 transition-transform">
                          <Upload className="w-6 h-6" />
                        </div>
                        <label className="text-sm font-extrabold text-gray-800 cursor-pointer hover:underline mb-1">
                          Drag & drop file here, or <span className="text-[#4F46E5]">browse files</span>
                          <input type="file" accept=".jpg,.jpeg,.png,.webp,.pdf" onChange={handleFileChange} className="hidden" />
                        </label>
                        <span className="text-[10px] text-gray-400 font-bold uppercase">JPG, JPEG, PNG, WEBP, PDF &bull; MAX 10 MB</span>
                      </div>
                    ) : (
                      <div className="border border-gray-200/80 rounded-2xl p-5 flex items-center justify-between bg-white shadow-sm">
                        <div className="flex items-center gap-3">
                          <div className="w-11 h-11 bg-indigo-50 border border-indigo-100 rounded-xl flex items-center justify-center text-[#4F46E5]">
                            {getFileIcon(file.type)}
                          </div>
                          <div>
                            <h4 className="text-sm font-extrabold text-gray-800 break-all">{file.name}</h4>
                            <p className="text-xs text-gray-450 font-semibold">{(file.size / (1024 * 1024)).toFixed(2)} MB</p>
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

                    {uploading && (
                      <div className="space-y-1.5">
                        <div className="flex justify-between text-xs font-bold text-gray-600">
                          <span>Uploading...</span>
                          <span>{uploadProgress}%</span>
                        </div>
                        <div className="w-full h-2 bg-gray-100 rounded-full overflow-hidden border border-gray-200/40">
                          <div className="h-full bg-[#4F46E5] transition-all duration-150" style={{ width: `${uploadProgress}%` }} />
                        </div>
                      </div>
                    )}

                    <button
                      onClick={handleUpload}
                      disabled={uploading || !file || !question.trim()}
                      className="w-full inline-flex items-center justify-center px-6 py-3.5 bg-[#4F46E5] hover:bg-[#4338CA] disabled:bg-gray-300 disabled:cursor-not-allowed text-white text-sm font-black rounded-2xl shadow-xl shadow-indigo-100/50 active:scale-[0.98] transition-all duration-150"
                    >
                      {uploading ? 'Uploading...' : 'Upload Answer'}
                      <ChevronRight className="ml-2 w-4.5 h-4.5" />
                    </button>
                  </div>
                </div>

                {/* SECTION 4: AI EVALUATION */}
                {submissionId && (
                  <div className="bg-white rounded-3xl border border-gray-200/80 shadow-sm overflow-hidden">
                    <div className="p-6 border-b border-gray-100">
                      <h2 className="text-lg font-extrabold text-gray-900 mb-1">AI Evaluation</h2>
                      <p className="text-xs text-gray-400 font-semibold">Get AI-powered feedback on your answer</p>
                    </div>

                    <div className="p-6 space-y-5">
                      {evaluationResult ? (
                        <div className="space-y-4">
                          <div className="bg-gradient-to-r from-indigo-500 to-indigo-600 text-white rounded-2xl p-6 shadow-lg">
                            <div className="flex items-center justify-between">
                              <div>
                                <p className="text-xs font-semibold text-indigo-100 uppercase tracking-wider mb-1">Score Obtained</p>
                                <p className="text-3xl font-black">{evaluationResult.score} / {evaluationResult.maxMarks}</p>
                              </div>
                              <div className="w-12 h-12 bg-white/20 rounded-full flex items-center justify-center">
                                <Award className="w-6 h-6" />
                              </div>
                            </div>
                          </div>

                          <div className="space-y-3">
                            <div className="bg-gray-50 rounded-xl p-4">
                              <p className="text-[10px] font-black text-gray-400 uppercase tracking-wider mb-2">Conceptual Accuracy</p>
                              <p className="text-sm font-semibold text-gray-800">{evaluationResult.conceptualAccuracy}</p>
                            </div>

                            <div className="bg-gray-50 rounded-xl p-4">
                              <p className="text-[10px] font-black text-gray-400 uppercase tracking-wider mb-2">Technical Correctness</p>
                              <p className="text-sm font-semibold text-gray-800">{evaluationResult.technicalCorrectness}</p>
                            </div>

                            <div className="bg-gray-50 rounded-xl p-4">
                              <p className="text-[10px] font-black text-gray-400 uppercase tracking-wider mb-2">Presentation</p>
                              <p className="text-sm font-semibold text-gray-800">{evaluationResult.presentation}</p>
                            </div>

                            {evaluationResult.diagramFeedback && evaluationResult.diagramFeedback !== 'No diagram' && (
                              <div className="bg-gray-50 rounded-xl p-4">
                                <p className="text-[10px] font-black text-gray-400 uppercase tracking-wider mb-2">Diagram Feedback</p>
                                <p className="text-sm font-semibold text-gray-800">{evaluationResult.diagramFeedback}</p>
                              </div>
                            )}
                          </div>

                          {evaluationResult.improvements && evaluationResult.improvements.length > 0 && (
                            <div className="space-y-2">
                              <p className="text-[10px] font-black text-orange-600 uppercase tracking-wider">Areas of Improvement</p>
                              <div className="space-y-2">
                                {evaluationResult.improvements.map((improvement, idx) => (
                                  <div key={idx} className="flex items-start gap-2 bg-orange-50 rounded-xl p-3">
                                    <AlertCircle className="w-4 h-4 text-orange-500 flex-shrink-0 mt-0.5" />
                                    <p className="text-xs font-semibold text-gray-800">{improvement}</p>
                                  </div>
                                ))}
                              </div>
                            </div>
                          )}
                        </div>
                      ) : (
                        <button
                          onClick={handleEvaluate}
                          disabled={evaluating}
                          className="w-full inline-flex items-center justify-center px-6 py-3.5 bg-[#10B981] hover:bg-[#059669] disabled:bg-gray-300 disabled:cursor-not-allowed text-white text-sm font-black rounded-2xl shadow-xl shadow-emerald-100/50 active:scale-[0.98] transition-all duration-150"
                        >
                          {evaluating ? (
                            <>
                              <Loader2 className="w-4.5 h-4.5 mr-2 animate-spin" />
                              Evaluating...
                            </>
                          ) : (
                            <>
                              <Sparkles className="w-4.5 h-4.5 mr-2" />
                              Start AI Evaluation
                            </>
                          )}
                        </button>
                      )}
                    </div>
                  </div>
                )}
              </div>

              {/* RIGHT COLUMN - EVALUATION HISTORY */}
              <div className="space-y-6">
                <div className="bg-white rounded-3xl border border-gray-200/80 shadow-sm overflow-hidden">
                  <div className="p-6 border-b border-gray-100">
                    <h2 className="text-lg font-extrabold text-gray-900 mb-1">Evaluation History</h2>
                    <p className="text-xs text-gray-400 font-semibold">View your previous evaluations</p>
                  </div>

                  <div className="p-6 space-y-5">
                    {/* Search */}
                    <div className="relative">
                      <Search className="absolute left-3 top-1/2 transform -translate-y-1/2 w-4 h-4 text-gray-400" />
                      <input
                        type="text"
                        value={searchQuery}
                        onChange={handleSearch}
                        placeholder="Search by question..."
                        className="w-full pl-10 pr-4 py-3 border border-gray-200 rounded-xl bg-white shadow-sm placeholder-gray-400 text-gray-800 text-sm font-medium focus:ring-4 focus:ring-[#4F46E5]/10 focus:border-[#4F46E5] outline-none transition-all duration-200"
                      />
                    </div>

                    {/* History List */}
                    {loadingHistory ? (
                      <div className="space-y-3">
                        {[1, 2, 3].map((i) => (
                          <div key={i} className="h-20 bg-gray-50 rounded-xl animate-pulse" />
                        ))}
                      </div>
                    ) : history.length === 0 ? (
                      <div className="text-center py-10">
                        <Clock className="w-12 h-12 text-gray-300 mx-auto mb-3" />
                        <p className="text-sm font-bold text-gray-500">No evaluations yet</p>
                        <p className="text-xs text-gray-400 mt-1">Upload your first answer to get started</p>
                      </div>
                    ) : (
                      <div className="space-y-3 max-h-[600px] overflow-y-auto">
                        {history.map((item) => (
                          <div
                            key={item.submissionId}
                            className="border border-gray-200 rounded-xl p-4 hover:border-indigo-200 hover:bg-indigo-50/30 transition-all cursor-pointer"
                            onClick={() => handleViewDetails(item)}
                          >
                            <div className="flex items-start justify-between gap-3">
                              <div className="flex-1 min-w-0">
                                <p className="text-sm font-extrabold text-gray-900 line-clamp-2 mb-2">{item.question}</p>
                                <div className="flex items-center gap-2 flex-wrap">
                                  {item.topic && (
                                    <span className="inline-flex items-center px-2 py-0.5 rounded-full bg-indigo-50 text-[10px] font-bold text-[#4F46E5]">
                                      {item.topic}
                                    </span>
                                  )}
                                  <span className="text-[10px] text-gray-400 font-semibold">
                                    {item.marksLimit} marks
                                  </span>
                                </div>
                              </div>
                              <div className="flex flex-col items-end gap-2 flex-shrink-0">
                                {item.score !== null ? (
                                  <span className={`inline-flex items-center px-2.5 py-1 rounded-full text-xs font-black ${
                                    (item.score / item.maxMarks) >= 0.8
                                      ? 'bg-emerald-50 text-emerald-700'
                                      : (item.score / item.maxMarks) >= 0.6
                                      ? 'bg-indigo-50 text-indigo-700'
                                      : 'bg-orange-50 text-orange-700'
                                  }`}>
                                    {item.score}/{item.maxMarks}
                                  </span>
                                ) : (
                                  <span className={`inline-flex items-center px-2.5 py-1 rounded-full text-xs font-black ${
                                    item.evaluationStatus === 'COMPLETED'
                                      ? 'bg-emerald-50 text-emerald-700'
                                      : item.evaluationStatus === 'FAILED'
                                      ? 'bg-red-50 text-red-700'
                                      : 'bg-gray-50 text-gray-600'
                                  }`}>
                                    {item.evaluationStatus}
                                  </span>
                                )}
                                <button className="p-1.5 text-gray-400 hover:text-[#4F46E5] hover:bg-indigo-50 rounded-lg transition-all">
                                  <Eye className="w-4 h-4" />
                                </button>
                              </div>
                            </div>
                          </div>
                        ))}
                      </div>
                    )}

                    {/* Pagination */}
                    {totalPages > 1 && (
                      <div className="flex items-center justify-between pt-4 border-t border-gray-100">
                        <button
                          onClick={() => setCurrentPage((prev) => Math.max(0, prev - 1))}
                          disabled={currentPage === 0}
                          className="inline-flex items-center px-4 py-2 border border-gray-200 rounded-lg text-sm font-bold text-gray-600 hover:bg-gray-50 disabled:opacity-50 disabled:cursor-not-allowed transition-all"
                        >
                          <ChevronLeft className="w-4 h-4 mr-1" />
                          Previous
                        </button>
                        <span className="text-xs font-bold text-gray-400">
                          Page {currentPage + 1} of {totalPages}
                        </span>
                        <button
                          onClick={() => setCurrentPage((prev) => Math.min(totalPages - 1, prev + 1))}
                          disabled={currentPage === totalPages - 1}
                          className="inline-flex items-center px-4 py-2 border border-gray-200 rounded-lg text-sm font-bold text-gray-600 hover:bg-gray-50 disabled:opacity-50 disabled:cursor-not-allowed transition-all"
                        >
                          Next
                          <ChevronRight className="w-4 h-4 ml-1" />
                        </button>
                      </div>
                    )}
                  </div>
                </div>
              </div>
            </div>
          </div>
        </main>
      </div>

      {/* VIEW DETAILS MODAL */}
      {showDetailsModal && selectedEvaluation && (
        <div className="fixed inset-0 bg-gray-900/50 backdrop-blur-sm z-50 flex items-center justify-center p-4">
          <div className="bg-white rounded-3xl max-w-2xl w-full max-h-[90vh] overflow-y-auto shadow-2xl">
            <div className="sticky top-0 bg-white border-b border-gray-100 p-6 flex items-center justify-between z-10">
              <h3 className="text-lg font-extrabold text-gray-900">Evaluation Details</h3>
              <button
                onClick={() => setShowDetailsModal(false)}
                className="p-2 text-gray-400 hover:bg-gray-100 rounded-xl transition-all"
              >
                <X className="w-5 h-5" />
              </button>
            </div>

            <div className="p-6 space-y-6">
              {/* File Preview */}
              <div className="bg-gray-50 rounded-xl p-4">
                <p className="text-[10px] font-black text-gray-400 uppercase tracking-wider mb-3">Uploaded File</p>
                <div className="flex items-center gap-3">
                  <div className="w-10 h-10 bg-indigo-50 border border-indigo-100 rounded-lg flex items-center justify-center text-[#4F46E5]">
                    {getFileIcon(selectedEvaluation.fileType)}
                  </div>
                  <div className="flex-1 min-w-0">
                    <p className="text-sm font-extrabold text-gray-800 truncate">{selectedEvaluation.originalFileName || 'Answer file'}</p>
                    <p className="text-xs text-gray-450 font-semibold">{selectedEvaluation.fileType}</p>
                  </div>
                </div>
              </div>

              {/* Score */}
              <div className="bg-gradient-to-r from-indigo-500 to-indigo-600 text-white rounded-2xl p-6 shadow-lg">
                <div className="flex items-center justify-between">
                  <div>
                    <p className="text-xs font-semibold text-indigo-100 uppercase tracking-wider mb-1">Marks Obtained</p>
                    <p className="text-3xl font-black">
                      {selectedEvaluation.score !== null ? `${selectedEvaluation.score} / ${selectedEvaluation.maxMarks}` : 'Not evaluated'}
                    </p>
                  </div>
                  <div className="w-12 h-12 bg-white/20 rounded-full flex items-center justify-center">
                    <Award className="w-6 h-6" />
                  </div>
                </div>
              </div>

              {/* Evaluation Breakdown */}
              {selectedEvaluation.score !== null && (
                <>
                  <div className="space-y-3">
                    <p className="text-[10px] font-black text-gray-400 uppercase tracking-wider">Evaluation Breakdown</p>
                    
                    <div className="bg-gray-50 rounded-xl p-4">
                      <p className="text-[10px] font-black text-gray-400 uppercase tracking-wider mb-2">Conceptual Accuracy</p>
                      <p className="text-sm font-semibold text-gray-800">{selectedEvaluation.conceptualAccuracy}</p>
                    </div>

                    <div className="bg-gray-50 rounded-xl p-4">
                      <p className="text-[10px] font-black text-gray-400 uppercase tracking-wider mb-2">Technical Correctness</p>
                      <p className="text-sm font-semibold text-gray-800">{selectedEvaluation.technicalCorrectness}</p>
                    </div>

                    <div className="bg-gray-50 rounded-xl p-4">
                      <p className="text-[10px] font-black text-gray-400 uppercase tracking-wider mb-2">Presentation</p>
                      <p className="text-sm font-semibold text-gray-800">{selectedEvaluation.presentation}</p>
                    </div>

                    {selectedEvaluation.diagramFeedback && selectedEvaluation.diagramFeedback !== 'No diagram' && (
                      <div className="bg-gray-50 rounded-xl p-4">
                        <p className="text-[10px] font-black text-gray-400 uppercase tracking-wider mb-2">Diagram Analysis</p>
                        <p className="text-sm font-semibold text-gray-800">{selectedEvaluation.diagramFeedback}</p>
                      </div>
                    )}
                  </div>

                  {/* Improvements */}
                  {selectedEvaluation.improvements && selectedEvaluation.improvements.length > 0 && (
                    <div className="space-y-2">
                      <p className="text-[10px] font-black text-orange-600 uppercase tracking-wider">Areas of Improvement</p>
                      <div className="space-y-2">
                        {selectedEvaluation.improvements.map((improvement, idx) => (
                          <div key={idx} className="flex items-start gap-2 bg-orange-50 rounded-xl p-3">
                            <AlertCircle className="w-4 h-4 text-orange-500 flex-shrink-0 mt-0.5" />
                            <p className="text-xs font-semibold text-gray-800">{improvement}</p>
                          </div>
                        ))}
                      </div>
                    </div>
                  )}

                  {/* Strengths */}
                  {selectedEvaluation.strengths && selectedEvaluation.strengths.length > 0 && (
                    <div className="space-y-2">
                      <p className="text-[10px] font-black text-emerald-600 uppercase tracking-wider">Strengths</p>
                      <div className="space-y-2">
                        {selectedEvaluation.strengths.map((strength, idx) => (
                          <div key={idx} className="flex items-start gap-2 bg-emerald-50 rounded-xl p-3">
                            <CheckCircle2 className="w-4 h-4 text-emerald-500 flex-shrink-0 mt-0.5" />
                            <p className="text-xs font-semibold text-gray-800">{strength}</p>
                          </div>
                        ))}
                      </div>
                    </div>
                  )}

                  {/* Weaknesses */}
                  {selectedEvaluation.weaknesses && selectedEvaluation.weaknesses.length > 0 && (
                    <div className="space-y-2">
                      <p className="text-[10px] font-black text-red-600 uppercase tracking-wider">Weaknesses</p>
                      <div className="space-y-2">
                        {selectedEvaluation.weaknesses.map((weakness, idx) => (
                          <div key={idx} className="flex items-start gap-2 bg-red-50 rounded-xl p-3">
                            <XCircle className="w-4 h-4 text-red-500 flex-shrink-0 mt-0.5" />
                            <p className="text-xs font-semibold text-gray-800">{weakness}</p>
                          </div>
                        ))}
                      </div>
                    </div>
                  )}

                  {/* Missing Keywords */}
                  {selectedEvaluation.keywordsMissing && selectedEvaluation.keywordsMissing.length > 0 && (
                    <div className="space-y-2">
                      <p className="text-[10px] font-black text-indigo-600 uppercase tracking-wider">Keywords Missing</p>
                      <div className="flex flex-wrap gap-2">
                        {selectedEvaluation.keywordsMissing.map((keyword, idx) => (
                          <span key={idx} className="inline-flex items-center px-3 py-1 rounded-lg bg-indigo-50 text-[10px] font-bold text-[#4F46E5]">
                            {keyword}
                          </span>
                        ))}
                      </div>
                    </div>
                  )}
                </>
              )}

              {/* Question */}
              <div className="bg-gray-50 rounded-xl p-4">
                <p className="text-[10px] font-black text-gray-400 uppercase tracking-wider mb-2">Question</p>
                <p className="text-sm font-semibold text-gray-800">{selectedEvaluation.question}</p>
                {selectedEvaluation.topic && (
                  <p className="text-xs text-gray-450 font-semibold mt-2">Topic: {selectedEvaluation.topic}</p>
                )}
              </div>
            </div>
          </div>
        </div>
      )}
    </div>
  );
};

export default AnswerEvaluatorPage;
