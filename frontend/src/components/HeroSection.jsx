import React from 'react'
import { ArrowRight, BarChart3, FileText, Zap } from 'lucide-react'
import Button from './ui/Button'
import { useNavigate } from 'react-router-dom'

const HeroSection = () => {
    const navigate = useNavigate()

  return (
    <section className="py-12 sm:py-16 md:py-20 lg:py-28 px-4 sm:px-6 lg:px-8">
      <div className="max-w-7xl mx-auto">
        <div className="grid grid-cols-1 lg:grid-cols-2 gap-12 lg:gap-16 items-center">
          <div className="space-y-6 md:space-y-8">
            <h1 className="text-4xl sm:text-5xl lg:text-6xl font-bold text-gray-900 leading-tight tracking-tight">
              Turn Your Notes Into AI-Powered Practice Tests
            </h1>

            <p className="text-lg sm:text-xl text-gray-600 leading-relaxed max-w-xl">
              Upload PDFs, generate custom exams instantly, and get professor-level evaluation for descriptive answers.
            </p>

            <div className="flex flex-col sm:flex-row gap-4 pt-2">
              <Button        onClick={() => navigate('/signup')}
icon={ArrowRight} aria-label="Get started with PrepPulse AI">
                Get Started
              </Button>
              <Button variant="secondary" className="px-8 py-4" aria-label="Watch product demo">
                Watch Demo
              </Button>
            </div>
          </div>

          <div className="relative">
            <div className="bg-gradient-to-br from-indigo-50 to-blue-50 rounded-2xl p-6 sm:p-8 border border-gray-200 shadow-xl">
              <div className="space-y-5">
                <div className="flex items-center justify-between">
                  <h3 className="text-lg font-semibold text-gray-900">Dashboard</h3>
                  <div className="flex gap-1.5" aria-hidden="true">
                    <div className="w-2.5 h-2.5 rounded-full bg-red-400" />
                    <div className="w-2.5 h-2.5 rounded-full bg-yellow-400" />
                    <div className="w-2.5 h-2.5 rounded-full bg-green-400" />
                  </div>
                </div>

                <div className="grid grid-cols-2 gap-4">
                  <div className="bg-white rounded-xl p-4 border border-gray-200 shadow-sm">
                    <div className="flex items-center gap-3 mb-2">
                      <div className="w-10 h-10 bg-indigo-100 rounded-lg flex items-center justify-center">
                        <Zap size={20} className="text-primary" aria-hidden="true" />
                      </div>
                      <span className="text-xs font-medium text-gray-500">Tests Created</span>
                    </div>
                    <p className="text-2xl font-bold text-gray-900">24</p>
                  </div>

                  <div className="bg-white rounded-xl p-4 border border-gray-200 shadow-sm">
                    <div className="flex items-center gap-3 mb-2">
                      <div className="w-10 h-10 bg-green-100 rounded-lg flex items-center justify-center">
                        <BarChart3 size={20} className="text-success" aria-hidden="true" />
                      </div>
                      <span className="text-xs font-medium text-gray-500">Avg. Score</span>
                    </div>
                    <p className="text-2xl font-bold text-gray-900">82%</p>
                  </div>
                </div>

                <div className="bg-white rounded-xl p-5 border border-gray-200 shadow-sm">
                  <p className="text-sm font-medium text-gray-700 mb-4">Weekly Performance</p>
                  <div className="flex items-end justify-between h-24 gap-2" aria-hidden="true">
                    <div className="w-full bg-indigo-200 rounded-t-md h-[55%]" />
                    <div className="w-full bg-indigo-300 rounded-t-md h-[70%]" />
                    <div className="w-full bg-indigo-400 rounded-t-md h-[80%]" />
                    <div className="w-full bg-primary rounded-t-md h-[92%]" />
                  </div>
                </div>

                <div className="bg-white rounded-xl p-4 border border-gray-200 shadow-sm">
                  <div className="flex items-center justify-between mb-3">
                    <span className="text-sm font-medium text-gray-700">Recent Test</span>
                    <span className="text-xs font-semibold text-success bg-green-50 px-2 py-1 rounded-full">Passed</span>
                  </div>
                  <div className="h-2 bg-gray-100 rounded-full overflow-hidden">
                    <div className="h-full w-4/5 bg-gradient-to-r from-primary to-indigo-400 rounded-full" />
                  </div>
                </div>

                <button
                  type="button"
                  className="w-full py-3 bg-primary text-white font-medium rounded-lg hover:bg-indigo-700 transition-colors duration-200 flex items-center justify-center gap-2"
                  aria-label="Upload new PDF"
                >
                  <FileText size={18} aria-hidden="true" />
                  Upload New PDF
                </button>
              </div>
            </div>

            <div className="absolute -bottom-6 -right-6 w-28 h-28 bg-success/10 rounded-full blur-2xl pointer-events-none" aria-hidden="true" />
            <div className="absolute -top-8 -left-8 w-36 h-36 bg-primary/5 rounded-full blur-3xl pointer-events-none" aria-hidden="true" />
          </div>
        </div>
      </div>
    </section>
  )
}

export default HeroSection
