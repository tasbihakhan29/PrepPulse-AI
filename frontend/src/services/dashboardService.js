import API from '../api/auth';

export const dashboardService = {
  /**
   * Fetches dashboard details.
   * If the backend endpoint is not yet implemented or available (404/network error),
   * it falls back to mock data tailored to the user's onboardingCompleted state.
   */
  getDashboardData: async (user) => {
    try {
      const response = await API.get('/api/dashboard');
      return response.data;
    } catch (error) {
      // In development mode or if backend hasn't implemented it yet, fallback to mock data
      const isDev = import.meta.env.DEV;
      const isNotFound = error.response?.status === 404;
      const isNetworkError = !error.response;

      if (isDev || isNotFound || isNetworkError) {
        console.warn('Dashboard API unavailable. Falling back to development mock data.');

        // If user is new (onboardingCompleted is false), return empty state data
        if (!user || user.onboardingCompleted === false) {
          return {
            stats: {
              totalTests: 0,
              averageScore: 0,
              studyStreak: 0,
              flashcardsGenerated: 0,
              aiEvaluations: 0,
            },
            performanceTrend: [],
            recentActivity: [],
            insights: {
              strongTopics: [],
              weakTopics: [],
            },
          };
        }

        // Returning user stats
        return {
          stats: {
            totalTests: 12,
            averageScore: 84.5,
            studyStreak: 5,
            flashcardsGenerated: 120,
            aiEvaluations: 8,
          },
          performanceTrend: [
            { id: 1, name: 'Cellular Biology', score: 75, date: '2026-06-10' },
            { id: 2, name: 'Organic Chem I', score: 82, date: '2026-06-11' },
            { id: 3, name: 'Web Dev Basics', score: 90, date: '2026-06-12' },
            { id: 4, name: 'Data Structures', score: 88, date: '2026-06-14' },
            { id: 5, name: 'System Design', score: 87, date: '2026-06-15' },
          ],
          recentActivity: [
            { id: 5, name: 'System Design', score: 87, date: '2026-06-15' },
            { id: 4, name: 'Data Structures', score: 88, date: '2026-06-14' },
            { id: 3, name: 'Web Dev Basics', score: 90, date: '2026-06-12' },
            { id: 2, name: 'Organic Chem I', score: 82, date: '2026-06-11' },
            { id: 1, name: 'Cellular Biology', score: 75, date: '2026-06-10' },
          ],
          insights: {
            strongTopics: ['Data Structures', 'Web Dev Basics', 'System Design'],
            weakTopics: ['Cellular Biology', 'Organic Chem I'],
          },
        };
      }
      throw error;
    }
  },
};
