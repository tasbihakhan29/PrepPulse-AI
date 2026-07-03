import API from './auth';

export const evaluatorApi = {
  uploadAnswer: async (file, question, topic, marksLimit) => {
    const formData = new FormData();
    formData.append('file', file);
    formData.append('question', question);
    if (topic) formData.append('topic', topic);
    formData.append('marksLimit', marksLimit);

    const response = await API.post('/api/evaluator/upload', formData);
    return response.data;
  },

  evaluateAnswer: async (submissionId) => {
    const formData = new FormData();
    formData.append('submissionId', submissionId);

    const response = await API.post('/api/evaluator/evaluate', formData);
    return response.data;
  },

  getEvaluationHistory: async (searchQuery = '', page = 0, size = 10) => {
    const params = new URLSearchParams();
    if (searchQuery) params.append('search', searchQuery);
    params.append('page', page);
    params.append('size', size);

    const response = await API.get(`/api/evaluator/history?${params.toString()}`);
    return response.data;
  },

  getEvaluationById: async (id) => {
    const response = await API.get(`/api/evaluator/${id}`);
    return response.data;
  },
};
