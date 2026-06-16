import React from 'react'
import logoImg from '../assets/logo.png'

const Logo = ({ size = 'md', showText = true, dark = false, className = '' }) => {
  const sizes = {
    sm: { img: 'w-7 h-7', text: 'text-base' },
    md: { img: 'w-8 h-8', text: 'text-lg' },
    lg: { img: 'w-10 h-10', text: 'text-xl' },
  }

  const { img, text } = sizes[size] || sizes.md

  return (
    <div className={`flex items-center gap-2.5 ${className}`}>
      <img
        src={logoImg}
        alt="PrepPulse AI logo"
        className={`${img} flex-shrink-0 object-contain`}
      />
      {showText && (
        <span className={`${text} font-semibold ${dark ? 'text-white' : 'text-gray-900'}`}>
          PrepPulse AI
        </span>
      )}
    </div>
  )
}

export default Logo
