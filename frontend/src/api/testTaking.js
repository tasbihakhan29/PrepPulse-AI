import API from './auth';

export const testTakingApi = {
  startTest: async (testId) => {
    const response = await API.post('/api/tests/start', { testId });
    return response.data;
  },

  saveAnswer: async (attemptId, questionId, selectedAnswer) => {
    const response = await API.post('/api/tests/answer', {
      attemptId,
      questionId,
      selectedAnswer,
    });
    return response.data;
  },

  submitTest: async (attemptId, tabSwitchCount = 0) => {
    const response = await API.post('/api/tests/submit', {
      attemptId,
      tabSwitchCount,
    });
    return response.data;
  },

  getTest: async (testId) => {
    const response = await API.get(`/api/tests/${testId}`);
    return response.data;
  },

  getResult: async (attemptId) => {
    const response = await API.get(`/api/results/${attemptId}`);
    return response.data;
  },

  getResultReview: async (attemptId) => {
    const response = await API.get(`/api/results/${attemptId}/review`);
    return response.data;
  },
};
