import React from 'react'
import { ArrowRight } from 'lucide-react'
import Button from './ui/Button'
import { useNavigate } from 'react-router-dom'

const CTABanner = () => {
      const navigate = useNavigate()
  
  return (
    <section className="py-16 sm:py-20 lg:py-28 px-4 sm:px-6 lg:px-8 bg-white">
      <div className="max-w-5xl mx-auto">
        <div className="bg-gradient-to-br from-primary to-indigo-600 rounded-2xl p-8 sm:p-12 lg:p-16 shadow-xl relative overflow-hidden">
          <div className="absolute top-0 right-0 w-72 h-72 bg-white/5 rounded-full blur-3xl -translate-y-1/2 translate-x-1/3 pointer-events-none" aria-hidden="true" />
          <div className="absolute bottom-0 left-0 w-64 h-64 bg-white/5 rounded-full blur-3xl translate-y-1/3 -translate-x-1/3 pointer-events-none" aria-hidden="true" />

          <div className="relative z-10 text-center space-y-6">
            <h2 className="text-3xl sm:text-4xl lg:text-5xl font-bold text-white leading-tight tracking-tight">
              Start Practicing Smarter Today
            </h2>

            <p className="text-lg sm:text-xl text-indigo-100 max-w-2xl mx-auto leading-relaxed">
              Generate personalized exams and improve faster with AI.
            </p>

            <div className="flex flex-col sm:flex-row gap-4 justify-center pt-4">
              <Button  onClick={() => navigate('/signup')}
              variant="white" icon={ArrowRight} className="px-8 py-4" aria-label="Create a free account">
                Create Free Account
              </Button>
              <Button variant="outlineWhite" className="px-8 py-4" aria-label="Login to your account">
                Login
              </Button>
            </div>
          </div>
        </div>
      </div>
    </section>
  )
}

export default CTABanner
