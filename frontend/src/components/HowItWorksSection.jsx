import React from 'react'
import { Upload, Wand2, BarChart3, Lightbulb } from 'lucide-react'

const steps = [
  {
    number: 1,
    icon: Upload,
    title: 'Upload Study Material',
    description: 'Upload your PDFs, notes, or textbooks in seconds.',
  },
  {
    number: 2,
    icon: Wand2,
    title: 'Generate Custom Test',
    description: 'AI builds a personalized exam from your study material.',
  },
  {
    number: 3,
    icon: BarChart3,
    title: 'Analyze Performance',
    description: 'Review scores, weak areas, and progress over time.',
  },
  {
    number: 4,
    icon: Lightbulb,
    title: 'Improve With AI Feedback',
    description: 'Get actionable feedback to strengthen your preparation.',
  },
]

const HowItWorksSection = () => {
  return (
    <section id="how-it-works" className="py-16 sm:py-20 lg:py-28 px-4 sm:px-6 lg:px-8 bg-background">
      <div className="max-w-7xl mx-auto">
        <div className="text-center mb-14 sm:mb-16">
          <h2 className="text-3xl sm:text-4xl lg:text-5xl font-bold text-gray-900 mb-4 tracking-tight">
            How It Works
          </h2>
          <p className="text-lg text-gray-600 max-w-2xl mx-auto">
            Four simple steps from study material to smarter practice.
          </p>
        </div>

        <div className="relative">
          <div
            className="hidden lg:block absolute top-[4.5rem] left-[12%] right-[12%] h-0.5 bg-gradient-to-r from-transparent via-primary/40 to-transparent"
            aria-hidden="true"
          />

          <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-6 lg:gap-5">
            {steps.map((step, index) => {
              const Icon = step.icon
              const isLast = index === steps.length - 1

              return (
                <div key={step.number} className="relative flex flex-col items-center">
                  <article className="w-full bg-white border border-gray-200 rounded-2xl p-7 text-center hover:shadow-lg hover:border-primary/20 transition-all duration-300 relative z-10">
                    <div className="inline-flex items-center justify-center w-14 h-14 bg-gradient-to-br from-primary to-indigo-600 text-white rounded-full mb-5 shadow-md">
                      <span className="text-xl font-bold">{step.number}</span>
                    </div>

                    <div className="flex justify-center mb-4">
                      <div className="w-12 h-12 bg-indigo-50 rounded-xl flex items-center justify-center">
                        <Icon size={22} className="text-primary" aria-hidden="true" />
                      </div>
                    </div>

                    <h3 className="text-lg font-semibold text-gray-900 mb-2">
                      {step.title}
                    </h3>

                    <p className="text-gray-600 text-sm leading-relaxed">
                      {step.description}
                    </p>
                  </article>

                  {!isLast && (
                    <div className="flex lg:hidden justify-center my-4 text-primary" aria-hidden="true">
                      <svg className="w-6 h-6" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                        <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M19 9l-7 7-7-7" />
                      </svg>
                    </div>
                  )}

                  {!isLast && (
                    <div
                      className="hidden lg:block absolute top-[4.5rem] -right-3 z-20 w-6 h-6 bg-primary rounded-full border-4 border-background"
                      aria-hidden="true"
                    />
                  )}
                </div>
              )
            })}
          </div>
        </div>
      </div>
    </section>
  )
}

export default HowItWorksSection
