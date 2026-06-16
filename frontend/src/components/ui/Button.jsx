import React from 'react'

const variants = {
  primary:
    'bg-primary text-white hover:bg-indigo-700 shadow-md hover:shadow-lg',
  secondary:
    'border-2 border-primary text-primary hover:bg-indigo-50',
  ghost:
    'text-primary hover:bg-indigo-50',
  white:
    'bg-white text-primary hover:bg-gray-100 shadow-lg hover:shadow-xl',
  outlineWhite:
    'border-2 border-white text-white hover:bg-white/10',
}

const Button = ({
  children,
  variant = 'primary',
  className = '',
  icon: Icon,
  iconPosition = 'right',
  ...props
}) => {
  return (
    <button
      className={`inline-flex items-center justify-center gap-2 px-6 py-2.5 font-semibold rounded-lg transition-all duration-200 transform hover:-translate-y-0.5 focus:outline-none focus-visible:ring-2 focus-visible:ring-primary focus-visible:ring-offset-2 ${variants[variant]} ${className}`}
      {...props}
    >
      {Icon && iconPosition === 'left' && <Icon size={18} aria-hidden="true" />}
      {children}
      {Icon && iconPosition === 'right' && <Icon size={18} aria-hidden="true" />}
    </button>
  )
}

export default Button
