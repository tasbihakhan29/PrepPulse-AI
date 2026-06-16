// import React from 'react';
// import { useNavigate, Link } from 'react-router-dom';
// import { useForm } from 'react-hook-form';
// import { GoogleLogin } from '@react-oauth/google';
// import { Mail, Lock, ArrowRight } from 'lucide-react';
// import { useAuth } from '../../context/AuthContext';
// import Logo from '../../components/Logo';
// import toast from 'react-hot-toast';

// const LoginPage = () => {
//   const { login, googleLogin } = useAuth();
//   const navigate = useNavigate();
//   const [submitting, setSubmitting] = React.useState(false);

//   const {
//     register,
//     handleSubmit,
//     formState: { errors },
//   } = useForm();

//   const onSubmit = async (data) => {
//     setSubmitting(true);
//     try {
//       await login(data.email, data.password);
//       navigate('/dashboard');
//     } catch (err) {
//       console.error(err);
//     } finally {
//       setSubmitting(false);
//     }
//   };

//   const handleGoogleSuccess = async (credentialResponse) => {
//     try {
//       await googleLogin(credentialResponse.credential);
//       navigate('/dashboard');
//     } catch (err) {
//       console.error(err);
//     }
//   };

//   const handleGoogleError = () => {
//     toast.error('Google Authentication failed. Please try again.');
//   };

//   return (
//     <div className="min-h-screen bg-gradient-to-br from-indigo-50/50 via-white to-indigo-50/50 flex flex-col justify-center py-12 sm:px-6 lg:px-8">
//       <div className="sm:mx-auto sm:w-full sm:max-w-md">
//         <Logo size="lg" className="justify-center mb-6" />
//         <h2 className="text-center text-3xl font-extrabold text-gray-900 tracking-tight">
//           Welcome back
//         </h2>
//         <p className="mt-2 text-center text-sm text-gray-600">
//           Or{' '}
//           <Link
//             to="/signup"
//             className="font-medium text-indigo-600 hover:text-indigo-500 transition-colors duration-150"
//           >
//             create a new account
//           </Link>
//         </p>
//       </div>

//       <div className="mt-8 sm:mx-auto sm:w-full sm:max-w-md">
//         <div className="bg-white py-8 px-4 shadow-xl shadow-indigo-100/40 border border-gray-100 sm:rounded-xl sm:px-10">
//           {/* Google Auth Button Container */}
//           <div className="flex flex-col items-center justify-center">
//             <GoogleLogin
//               onSuccess={handleGoogleSuccess}
//               onError={handleGoogleError}
//               useOneTap
//               theme="outline"
//               size="large"
//               shape="rectangular"
//               text="continue_with"
//               width="360"
//             />
//           </div>

//           <div className="mt-6">
//             <div className="relative">
//               <div className="absolute inset-0 flex items-center">
//                 <div className="w-full border-t border-gray-200" />
//               </div>
//               <div className="relative flex justify-center text-sm">
//                 <span className="px-2 bg-white text-gray-500 font-medium">Or continue with email</span>
//               </div>
//             </div>
//           </div>

//           <form className="mt-6 space-y-6" onSubmit={handleSubmit(onSubmit)}>
//             <div>
//               <label htmlFor="email" className="block text-sm font-medium text-gray-700">
//                 Email address
//               </label>
//               <div className="mt-1 relative rounded-md shadow-sm">
//                 <div className="absolute inset-y-0 left-0 pl-3 flex items-center pointer-events-none">
//                   <Mail className="h-5 w-5 text-gray-400" aria-hidden="true" />
//                 </div>
//                 <input
//                   id="email"
//                   type="email"
//                   autoComplete="email"
//                   className={`block w-full pl-10 pr-3 py-2.5 border ${
//                     errors.email ? 'border-red-300 focus:ring-red-500 focus:border-red-500' : 'border-gray-300 focus:ring-indigo-500 focus:border-indigo-500'
//                   } rounded-lg bg-gray-50/30 placeholder-gray-400 focus:outline-none focus:ring-2 transition duration-150 text-sm`}
//                   placeholder="you@example.com"
//                   {...register('email', {
//                     required: 'Email is required',
//                     pattern: {
//                       value: /^[A-Z0-9._%+-]+@[A-Z0-9.-]+\.[A-Z]{2,}$/i,
//                       message: 'Invalid email address',
//                     },
//                   })}
//                 />
//               </div>
//               {errors.email && (
//                 <p className="mt-1.5 text-xs text-red-600 font-medium">{errors.email.message}</p>
//               )}
//             </div>

//             <div>
//               <div className="flex items-center justify-between">
//                 <label htmlFor="password" className="block text-sm font-medium text-gray-700">
//                   Password
//                 </label>
//                 <div className="text-sm">
//                   <Link
//                     to="/forgot-password"
//                     className="font-medium text-indigo-600 hover:text-indigo-500 transition-colors duration-150"
//                   >
//                     Forgot password?
//                   </Link>
//                 </div>
//               </div>
//               <div className="mt-1 relative rounded-md shadow-sm">
//                 <div className="absolute inset-y-0 left-0 pl-3 flex items-center pointer-events-none">
//                   <Lock className="h-5 w-5 text-gray-400" aria-hidden="true" />
//                 </div>
//                 <input
//                   id="password"
//                   type="password"
//                   autoComplete="current-password"
//                   className={`block w-full pl-10 pr-3 py-2.5 border ${
//                     errors.password ? 'border-red-300 focus:ring-red-500 focus:border-red-500' : 'border-gray-300 focus:ring-indigo-500 focus:border-indigo-500'
//                   } rounded-lg bg-gray-50/30 placeholder-gray-400 focus:outline-none focus:ring-2 transition duration-150 text-sm`}
//                   placeholder="••••••••"
//                   {...register('password', {
//                     required: 'Password is required',
//                   })}
//                 />
//               </div>
//               {errors.password && (
//                 <p className="mt-1.5 text-xs text-red-600 font-medium">{errors.password.message}</p>
//               )}
//             </div>

//             <div>
//               <button
//                 type="submit"
//                 disabled={submitting}
//                 className="w-full flex justify-center items-center py-2.5 px-4 border border-transparent rounded-lg shadow-sm text-sm font-semibold text-white bg-indigo-600 hover:bg-indigo-700 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-indigo-500 disabled:opacity-50 transition-all duration-150 hover:shadow-indigo-100"
//               >
//                 {submitting ? (
//                   <div className="h-5 w-5 border-2 border-white border-t-transparent rounded-full animate-spin"></div>
//                 ) : (
//                   <>
//                     Sign In
//                     <ArrowRight className="ml-2 h-4 w-4" />
//                   </>
//                 )}
//               </button>
//             </div>
//           </form>
//         </div>
//       </div>
//     </div>
//   );
// };

// export default LoginPage;
import React from 'react';
import { useNavigate, Link } from 'react-router-dom';
import { useForm } from 'react-hook-form';
import { GoogleLogin } from '@react-oauth/google';
import { Mail, Lock, ArrowRight, CheckCircle2, Sparkles } from 'lucide-react';
import { useAuth } from '../../context/AuthContext';
import Logo from '../../components/Logo';
import toast from 'react-hot-toast';

const LoginPage = () => {
  const { login, googleLogin } = useAuth();
  const navigate = useNavigate();
  const [submitting, setSubmitting] = React.useState(false);

  const {
    register,
    handleSubmit,
    formState: { errors },
  } = useForm();

  const onSubmit = async (data) => {
    setSubmitting(true);
    try {
      await login(data.email, data.password);
      navigate('/dashboard');
    } catch (err) {
      console.error(err);
    } finally {
      setSubmitting(false);
    }
  };

  const handleGoogleSuccess = async (credentialResponse) => {
    try {
      await googleLogin(credentialResponse.credential);
      navigate('/dashboard');
    } catch (err) {
      console.error(err);
    }
  };

  const handleGoogleError = () => {
    toast.error('Google Authentication failed. Please try again.');
  };

  return (
    <div className="min-h-screen bg-[#F9FAFB] flex flex-col md:flex-row font-sans antialiased overflow-hidden">
      
      {/* LEFT SECTION - Animated Gradient Sidebar */}
      <div className="hidden md:flex md:w-[60%] relative overflow-hidden flex-col justify-between p-16 lg:p-24 shadow-[inset_-10px_0_30px_rgba(0,0,0,0.1)]">
        
        {/* Animated Gradient Background Wrapper */}
        <div className="absolute inset-0 z-0 bg-[#4F46E5]">
          {/* Animated Blob 1 */}
          <div className="absolute top-[-10%] left-[-10%] w-[80%] h-[80%] bg-[#6366F1] rounded-full mix-blend-multiply filter blur-[120px] opacity-70 animate-blob" />
          {/* Animated Blob 2 */}
          <div className="absolute bottom-[-10%] right-[-10%] w-[70%] h-[70%] bg-[#4338CA] rounded-full mix-blend-multiply filter blur-[100px] opacity-70 animate-blob animation-delay-2000" />
          {/* Animated Blob 3 (Success Mint Accent) */}
          <div className="absolute top-[20%] right-[-5%] w-[50%] h-[50%] bg-[#10B981]/20 rounded-full mix-blend-overlay filter blur-[80px] animate-blob animation-delay-4000" />
        </div>

        {/* Noise Overlay for Texture */}
        <div className="absolute inset-0 opacity-[0.03] pointer-events-none bg-[url('https://grainy-gradients.vercel.app/noise.svg')]" />

        {/* Content remains centered and clear */}
        <div className="relative z-10 flex items-center">
          <Logo size="lg" className="text-white drop-shadow-sm" />
        </div>

        <div className="relative z-10 my-auto max-w-xl">
          <div className="inline-flex items-center gap-2 px-3 py-1 rounded-full bg-white/10 border border-white/20 text-white text-xs font-semibold mb-8 tracking-wide uppercase backdrop-blur-xl shadow-lg">
            <Sparkles className="w-3.5 h-3.5 text-[#10B981]" />
            <span>AI-Driven Learning Experience</span>
          </div>
          
          <h1 className="text-4xl lg:text-5xl xl:text-7xl font-black text-white tracking-tight leading-[1.05] mb-8 drop-shadow-md">
            Master Any Subject <br />
            <span className="text-transparent bg-clip-text bg-gradient-to-r from-white to-indigo-200">
              In Record Time.
            </span>
          </h1>
          
          <p className="text-indigo-50/80 text-lg lg:text-xl mb-12 leading-relaxed max-w-md font-medium">
            Generate smart practice tests from your notes. Track your progress with high-fidelity analytics.
          </p>

          <div className="grid grid-cols-1 sm:grid-cols-2 gap-y-6 gap-x-8">
            {[
              "AI Generated Tests",
              "Professor-Level Evaluation",
              "Detailed Analytics",
              "Knowledge Gap Insights"
            ].map((feature, index) => (
              <div key={index} className="flex items-center gap-3 group cursor-default">
                <div className="w-6 h-6 rounded-lg bg-white/10 border border-white/20 flex items-center justify-center transition-all duration-300 group-hover:bg-[#10B981]/20 group-hover:border-[#10B981]/40">
                  <CheckCircle2 className="w-4 h-4 text-[#10B981]" />
                </div>
                <span className="text-white font-semibold text-sm tracking-wide">{feature}</span>
              </div>
            ))}
          </div>
        </div>

        <div className="relative z-10 text-xs text-indigo-100/50 font-medium tracking-widest uppercase">
          Enterprise Security Standard &bull; 2026
        </div>
      </div>

      {/* RIGHT SECTION - Pure White Minimalist Form */}
      <div className="w-full md:w-[40%] flex flex-col justify-center items-center p-8 sm:p-12 lg:p-24 bg-[#F9FAFB] relative z-20">
        
        <div className="w-full max-w-[400px]">
          <div className="mb-10">
            <h2 className="text-4xl font-black tracking-tighter text-[#111827] mb-3">
              WELCOME BACK 
            </h2>
            <p className="text-base text-[#6B7280] font-medium">
              Don't have an account?{' '}
              <Link
                to="/signup"
                className="font-bold text-[#4F46E5] hover:underline decoration-2 underline-offset-4"
              >
                Join Now
              </Link>
            </p>
          </div>

          <div className="w-full group mb-8">
            <div className="w-full border border-gray-200/80 rounded-2xl overflow-hidden flex justify-center hover:border-gray-300 bg-white shadow-sm hover:shadow-md transition-all duration-300 p-0.5">
              <GoogleLogin
                onSuccess={handleGoogleSuccess}
                onError={handleGoogleError}
                useOneTap
                theme="neutral"
                size="large"
                shape="rectangular"
                text="continue_with"
                width="100%"
              />
            </div>
          </div>

          <div className="relative mb-8">
            <div className="absolute inset-0 flex items-center">
              <div className="w-full border-t border-gray-200/60" />
            </div>
            <div className="relative flex justify-center text-[10px]">
              <span className="px-4 bg-[#F9FAFB] text-[#6B7280] font-bold uppercase tracking-[0.2em]">
                Secure Email Access
              </span>
            </div>
          </div>

          <form className="space-y-6" onSubmit={handleSubmit(onSubmit)}>
            <div>
              <label htmlFor="email" className="block text-[11px] font-black text-[#111827] uppercase tracking-wider mb-2 ml-1">
                Email Address
              </label>
              <div className="relative group">
                <div className="absolute inset-y-0 left-0 pl-4 flex items-center pointer-events-none">
                  <Mail className="h-4 w-4 text-[#6B7280] group-focus-within:text-[#4F46E5] transition-colors" />
                </div>
                <input
                  id="email"
                  type="email"
                  {...register('email', { required: 'Email is required' })}
                  className="block w-full pl-11 pr-4 py-3.5 border border-gray-200 rounded-2xl bg-white shadow-sm placeholder-gray-400 text-[#111827] text-sm font-medium focus:ring-4 focus:ring-[#4F46E5]/10 focus:border-[#4F46E5] outline-none transition-all duration-200"
                  placeholder="name@example.com"
                />
              </div>
              {errors.email && <p className="mt-2 text-xs text-[#EF4444] font-bold ml-1">{errors.email.message}</p>}
            </div>

            <div>
              <div className="flex items-center justify-between mb-2 ml-1">
                <label htmlFor="password" className="block text-[11px] font-black text-[#111827] uppercase tracking-wider">
                  Password
                </label>
                <Link to="/forgot-password" size="sm" className="text-xs font-bold text-[#4F46E5] hover:text-[#4F46E5]/80 transition-colors">
                  Reset?
                </Link>
              </div>
              <div className="relative group">
                <div className="absolute inset-y-0 left-0 pl-4 flex items-center pointer-events-none">
                  <Lock className="h-4 w-4 text-[#6B7280] group-focus-within:text-[#4F46E5] transition-colors" />
                </div>
                <input
                  id="password"
                  type="password"
                  {...register('password', { required: 'Password is required' })}
                  className="block w-full pl-11 pr-4 py-3.5 border border-gray-200 rounded-2xl bg-white shadow-sm placeholder-gray-400 text-[#111827] text-sm font-medium focus:ring-4 focus:ring-[#4F46E5]/10 focus:border-[#4F46E5] outline-none transition-all duration-200"
                  placeholder="••••••••"
                />
              </div>
              {errors.password && <p className="mt-2 text-xs text-[#EF4444] font-bold ml-1">{errors.password.message}</p>}
            </div>

            <button
              type="submit"
              disabled={submitting}
              className="w-full h-14 flex justify-center items-center px-6 rounded-2xl shadow-xl shadow-[#4F46E5]/20 text-base font-black text-white bg-[#4F46E5] hover:bg-[#4338CA] active:scale-[0.97] transition-all duration-200 group"
            >
              {submitting ? (
                <div className="h-6 w-6 border-3 border-white/30 border-t-white rounded-full animate-spin" />
              ) : (
                <span className="flex items-center gap-2">
                  Continue to Workspace
                  <ArrowRight className="h-5 w-5 transition-transform group-hover:translate-x-1" />
                </span>
              )}
            </button>
          </form>
        </div>
      </div>

      {/* Add this to your Global CSS or Tailwind Config for the animation to work */}
      <style dangerouslySetInnerHTML={{ __html: `
        @keyframes blob {
          0% { transform: translate(0px, 0px) scale(1); }
          33% { transform: translate(30px, -50px) scale(1.1); }
          66% { transform: translate(-20px, 20px) scale(0.9); }
          100% { transform: translate(0px, 0px) scale(1); }
        }
        .animate-blob {
          animation: blob 7s infinite;
        }
        .animation-delay-2000 {
          animation-delay: 2s;
        }
        .animation-delay-4000 {
          animation-delay: 4s;
        }
      `}} />
    </div>
  );
};

export default LoginPage;