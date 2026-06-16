import React from 'react'
import Navbar from '../components/Navbar'
import HeroSection from '../components/HeroSection'
import FeaturesSection from '../components/FeaturesSection'
import HowItWorksSection from '../components/HowItWorksSection'
import CTABanner from '../components/CTABanner'
import Footer from '../components/Footer'

const LandingPage = () => {
  return (
    <div className="min-h-screen bg-background">
      <Navbar />
      <main>
        <HeroSection />
        <FeaturesSection />
        <HowItWorksSection />
        <CTABanner />
      </main>
      <Footer />
    </div>
  )
}

export default LandingPage 
