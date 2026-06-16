import React, { useState } from 'react'
import { Menu, X } from 'lucide-react'
import Logo from './Logo'
import Button from './ui/Button'
import { useNavigate } from 'react-router-dom'

const Navbar = () => {
  const [isOpen, setIsOpen] = useState(false)

  const closeMenu = () => setIsOpen(false)
    const navigate = useNavigate()

  return (
    <nav className="sticky top-0 z-50 bg-white/95 backdrop-blur-sm border-b border-gray-100 shadow-sm">
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
        <div className="flex justify-between items-center h-16">
          <a href="#" className="flex-shrink-0" aria-label="PrepPulse AI home">
            <Logo />
          </a>

          <div className="hidden md:flex items-center gap-4">
            <Button       onClick={() => navigate('/login')}

            variant="ghost" aria-label="Login to your account">
              Login
            </Button>
            <Button        onClick={() => navigate('/signup')}

            aria-label="Get started with PrepPulse AI">
              Get Started
            </Button>
          </div>

          <button
            type="button"
            onClick={() => setIsOpen(!isOpen)}
            className="md:hidden p-2 rounded-lg text-gray-700 hover:bg-gray-100 transition-colors duration-200 focus:outline-none focus-visible:ring-2 focus-visible:ring-primary"
            aria-expanded={isOpen}
            aria-label={isOpen ? 'Close menu' : 'Open menu'}
          >
            {isOpen ? <X size={24} /> : <Menu size={24} />}
          </button>
        </div>

        {isOpen && (
          <div className="md:hidden border-t border-gray-100 pb-4">
            <div className="flex flex-col gap-3 pt-4">
              <Button variant="ghost" className="w-full" onClick={closeMenu}>
                Login
              </Button>
              <Button className="w-full" onClick={closeMenu}>
                Get Started
              </Button>
            </div>
          </div>
        )}
      </div>
    </nav>
  )
}

export default Navbar
