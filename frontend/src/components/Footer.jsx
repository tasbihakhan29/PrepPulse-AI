import React from 'react'
import Logo from './Logo'

const footerLinks = [
  { label: 'Features', href: '#features' },
  { label: 'Login', href: '#login' },
  { label: 'Privacy Policy', href: '#privacy' },
  { label: 'Contact', href: '#contact' },
]

const Footer = () => {
  return (
    <footer className="bg-gray-900 text-gray-300">
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-10 sm:py-12">
        <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-8">
          <div>
            <Logo size="lg" dark />
          </div>

          <nav aria-label="Footer navigation">
            <ul className="flex flex-wrap gap-x-6 gap-y-3 sm:justify-end">
              {footerLinks.map((link) => (
                <li key={link.label}>
                  <a
                    href={link.href}
                    className="text-gray-400 hover:text-white transition-colors duration-200 text-sm font-medium"
                  >
                    {link.label}
                  </a>
                </li>
              ))}
            </ul>
          </nav>
        </div>

        <div className="mt-8 pt-8 border-t border-gray-800">
          <p className="text-gray-500 text-sm text-center sm:text-left">
            © {new Date().getFullYear()} PrepPulse AI. All rights reserved.
          </p>
        </div>
      </div>
    </footer>
  )
}

export default Footer
