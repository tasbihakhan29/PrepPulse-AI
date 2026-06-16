import React from 'react'
import { Sparkles, BookOpen, TrendingUp, PenLine } from 'lucide-react'

const features = [
  {
    icon: Sparkles,
    title: 'AI Practice Center',
    description: 'Generate tests from PDFs and notes.',
  },
  {
    icon: BookOpen,
    title: 'Custom Exam Builder',
    description: 'Create tests for GATE, UPSC, University exams, or custom exams.',
  },
  {
    icon: TrendingUp,
    title: 'Smart Analytics',
    description: 'Track scores, streaks, and performance trends.',
  },
  {
    icon: PenLine,
    title: '7-Mark Evaluator',
    description: 'Evaluate handwritten answers and diagrams using AI.',
  },
]

const FeaturesSection = () => {
  return (
    <section id="features" className="py-16 sm:py-20 lg:py-28 px-4 sm:px-6 lg:px-8 bg-white">
      <div className="max-w-7xl mx-auto">
        <div className="text-center mb-14 sm:mb-16">
          <h2 className="text-3xl sm:text-4xl lg:text-5xl font-bold text-gray-900 mb-4 tracking-tight">
            Everything You Need to Succeed
          </h2>
          <p className="text-lg text-gray-600 max-w-2xl mx-auto">
            Powerful tools built for students who want to study smarter and score higher.
          </p>
        </div>

        <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-6 lg:gap-8">
          {features.map((feature) => {
            const Icon = feature.icon
            return (
              <article
                key={feature.title}
                className="group bg-background border border-gray-200 rounded-2xl p-7 sm:p-8 hover:shadow-xl hover:border-primary/20 transition-all duration-300 hover:-translate-y-1"
              >
                <div className="w-14 h-14 bg-indigo-100 rounded-xl flex items-center justify-center mb-6 group-hover:bg-primary transition-colors duration-300">
                  <Icon size={26} className="text-primary group-hover:text-white transition-colors duration-300" aria-hidden="true" />
                </div>

                <h3 className="text-xl font-semibold text-gray-900 mb-3">
                  {feature.title}
                </h3>

                <p className="text-gray-600 leading-relaxed">
                  {feature.description}
                </p>
              </article>
            )
          })}
        </div>
      </div>
    </section>
  )
}

export default FeaturesSection
