import API from './auth';

export const historyAnalyticsApi = {
  getAnalyticsOverview: async () => {
    const response = await API.get('/api/history/analytics');
    return response.data;
  },

  getPerformanceChartData: async (days = 30) => {
    const response = await API.get('/api/history/performance-chart', { params: { days } });
    return response.data;
  },

  getTopicPerformance: async () => {
    const response = await API.get('/api/history/topic-performance');
    return response.data;
  },

  getHeatmapData: async (days = 365) => {
    const response = await API.get('/api/history/heatmap', { params: { days } });
    return response.data;
  },

  getQuestionTypeDistribution: async () => {
    const response = await API.get('/api/history/question-type-distribution');
    return response.data;
  },

  getDifficultyAnalysis: async () => {
    const response = await API.get('/api/history/difficulty-analysis');
    return response.data;
  },

  getLearningInsights: async () => {
    const response = await API.get('/api/history/insights');
    return response.data;
  },

  getRecommendations: async () => {
    const response = await API.get('/api/history/recommendations');
    return response.data;
  },

  getTestHistory: async (options = {}, legacySize, legacySearch, legacyExamType, legacyQuestionType, legacyDifficulty) => {
    const normalized = typeof options === 'object' && options !== null
      ? options
      : { page: options, size: legacySize, search: legacySearch, examType: legacyExamType, questionType: legacyQuestionType, difficulty: legacyDifficulty };
    const { page = 0, size = 10, search, examType, questionType, difficulty } = normalized;
    const response = await API.get('/api/history/tests', { params: { page, size, search, examType, questionType, difficulty } });
    return response.data;
  },

  getEvaluationHistory: async (options = {}, legacyPage, legacySize) => {
    const normalized = typeof options === 'object' && options !== null
      ? options
      : { search: options, page: legacyPage, size: legacySize };
    const { search = '', topic, status, page = 0, size = 10 } = normalized;
    const response = await API.get('/api/history/evaluations', { params: { search, topic, status, page, size } });
    return response.data;
  },

  exportCsv: async () => {
    const response = await API.get('/api/history/export/csv', { responseType: 'blob' });
    return response.data;
  },

  exportPdf: async () => {
    const response = await API.get('/api/history/export/pdf', { responseType: 'blob' });
    return response.data;
  },

  deleteTestHistory: async (attemptId) => {
    const response = await API.delete(`/api/history/tests/${attemptId}`);
    return response.data;
  },

  deleteEvaluationHistory: async (submissionId) => {
    const response = await API.delete(`/api/history/evaluations/${submissionId}`);
    return response.data;
  },

};
