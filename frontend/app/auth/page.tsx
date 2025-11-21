'use client'

import { useState, useEffect } from 'react'
import { useRouter, useSearchParams } from 'next/navigation'
import { Button } from '@/components/ui/button'
import { Card } from '@/components/ui/card'
import { Input } from '@/components/ui/input'
import { apiRequest } from '@/lib/api-config'

export default function AuthPage() {
  const router = useRouter()
  const searchParams = useSearchParams()
  const [role, setRole] = useState<'driver' | 'rider'>('driver')
  const [isLogin, setIsLogin] = useState(true)
  const [formData, setFormData] = useState({
    username: '',
    password: '',
    confirmPassword: '',
    licenseId: '',
  })
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState('')
  const [success, setSuccess] = useState('')

  useEffect(() => {
    const roleParam = searchParams.get('role') as 'driver' | 'rider' | null
    if (roleParam) {
      setRole(roleParam)
    }
  }, [searchParams])

  const handleInputChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    const { name, value } = e.target
    setFormData(prev => ({ ...prev, [name]: value }))
    setError('')
    setSuccess('')
  }

  const validateForm = () => {
    if (!formData.username || !formData.password) {
      setError('Username and password are required')
      return false
    }

    if (!isLogin) {
      if (formData.password !== formData.confirmPassword) {
        setError('Passwords do not match')
        return false
      }
      if (formData.password.length < 6) {
        setError('Password must be at least 6 characters')
        return false
      }
      if (role === 'driver' && !formData.licenseId) {
        setError('License ID is required for drivers')
        return false
      }
    }

    return true
  }

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault()
    setLoading(true)
    setError('')
    setSuccess('')

    if (!validateForm()) {
      setLoading(false)
      return
    }

    try {
      const endpoint = isLogin 
        ? `/api/user/login-${role}`
        : `/api/user/add-${role}`

      let payload: any = {
        username: formData.username,
        password: formData.password
      }

      // Add licenseId for driver signup
      if (!isLogin && role === 'driver') {
        payload.licenseId = parseInt(formData.licenseId)
      }

      // Use apiRequest instead of fetch
      const response = await apiRequest(endpoint, {
        method: 'POST',
        body: JSON.stringify(payload),
      })
        const data = await response.json()
        console.log(data)

      if (isLogin) {
        // Login flow - expects token in response
        if (response.ok && data.token) {
          localStorage.setItem('authToken', data.token)
          localStorage.setItem('username', formData.username)
          localStorage.setItem('role', role)
          localStorage.setItem('Id', data.userId)
          
          setSuccess('Login successful! Redirecting...')
          
          // Redirect to appropriate dashboard
          setTimeout(() => {
            router.push(`/${role}`)
          }, 1000)
        } else {
          throw new Error(data.message || 'Login failed')
        }
      } else {
        // Signup flow - expects STATUS_CODE
        if (data.status_CODE === 200) {
          setSuccess('Account created successfully! Please login.')
          
          // Clear form and switch to login
          setFormData({
            username: '',
            password: '',
            confirmPassword: '',
            licenseId: '',
          })
          
          setTimeout(() => {
            setIsLogin(true)
            setSuccess('')
          }, 2000)
        } else if (data.status_CODE === 409) {
          throw new Error('Username already exists')
        } else {
          throw new Error('Signup failed. Please try again.')
        }
      }

    } catch (err) {
      setError(err instanceof Error ? err.message : 'An error occurred')
    } finally {
      setLoading(false)
    }
  }

  return (
    <div className="min-h-screen bg-linear-to-br from-background to-muted flex items-center justify-center px-4">
      <div className="w-full max-w-md">
        {/* Logo */}
        <div className="text-center mb-8">
          <div className="w-12 h-12 rounded-lg bg-primary flex items-center justify-center mx-auto mb-3 text-2xl">
            🚗
          </div>
          <h1 className="text-2xl font-bold text-primary">OneRide</h1>
        </div>

        <Card className="p-8 shadow-lg">
          <div className="flex gap-2 mb-6">
            <button
              onClick={() => {
                setRole('driver')
                setFormData({
                  username: '',
                  password: '',
                  confirmPassword: '',
                  licenseId: '',
                })
                setError('')
                setSuccess('')
              }}
              className={`flex-1 py-2 px-3 rounded-lg font-medium transition-all flex items-center justify-center gap-2 ${
                role === 'driver'
                  ? 'bg-primary text-primary-foreground'
                  : 'bg-muted text-muted-foreground hover:bg-muted/80'
              }`}
            >
              <span className="text-lg">🚗</span>
              Driver
            </button>
            <button
              onClick={() => {
                setRole('rider')
                setFormData({
                  username: '',
                  password: '',
                  confirmPassword: '',
                  licenseId: '',
                })
                setError('')
                setSuccess('')
              }}
              className={`flex-1 py-2 px-3 rounded-lg font-medium transition-all flex items-center justify-center gap-2 ${
                role === 'rider'
                  ? 'bg-accent text-accent-foreground'
                  : 'bg-muted text-muted-foreground hover:bg-muted/80'
              }`}
            >
              <span className="text-lg">👤</span>
              Rider
            </button>
          </div>

          {/* Tab Toggle */}
          <div className="flex gap-2 mb-6 border-b border-border">
            <button
              onClick={() => {
                setIsLogin(true)
                setError('')
                setSuccess('')
              }}
              className={`flex-1 py-2 font-medium transition-colors ${
                isLogin
                  ? 'text-primary border-b-2 border-primary'
                  : 'text-muted-foreground'
              }`}
            >
              Login
            </button>
            <button
              onClick={() => {
                setIsLogin(false)
                setError('')
                setSuccess('')
              }}
              className={`flex-1 py-2 font-medium transition-colors ${
                !isLogin
                  ? 'text-primary border-b-2 border-primary'
                  : 'text-muted-foreground'
              }`}
            >
              Sign Up
            </button>
          </div>

          {/* Form */}
          <form onSubmit={handleSubmit} className="space-y-4">
            <div>
              <label className="text-sm font-medium text-foreground block mb-2">
                Username
              </label>
              <Input
                type="text"
                name="username"
                placeholder="Enter your username"
                value={formData.username}
                onChange={handleInputChange}
                required
                autoComplete="username"
              />
            </div>

            <div>
              <label className="text-sm font-medium text-foreground block mb-2">
                Password
              </label>
              <Input
                type="password"
                name="password"
                placeholder="••••••••"
                value={formData.password}
                onChange={handleInputChange}
                required
                autoComplete={isLogin ? "current-password" : "new-password"}
              />
            </div>

            {!isLogin && (
              <>
                <div>
                  <label className="text-sm font-medium text-foreground block mb-2">
                    Confirm Password
                  </label>
                  <Input
                    type="password"
                    name="confirmPassword"
                    placeholder="••••••••"
                    value={formData.confirmPassword}
                    onChange={handleInputChange}
                    required
                    autoComplete="new-password"
                  />
                </div>

                {role === 'driver' && (
                  <div>
                    <label className="text-sm font-medium text-foreground block mb-2">
                      License ID
                    </label>
                    <Input
                      type="number"
                      name="licenseId"
                      placeholder="Enter your license ID"
                      value={formData.licenseId}
                      onChange={handleInputChange}
                      required
                    />
                  </div>
                )}
              </>
            )}

            {error && (
              <div className="p-3 bg-destructive/10 border border-destructive/20 rounded-lg text-destructive text-sm">
                {error}
              </div>
            )}

            {success && (
              <div className="p-3 bg-green-50 dark:bg-green-900/20 border border-green-200 dark:border-green-800 rounded-lg text-green-700 dark:text-green-400 text-sm">
                {success}
              </div>
            )}

            <Button
              type="submit"
              disabled={loading}
              className="w-full"
            >
              {loading ? 'Loading...' : isLogin ? 'Sign In' : 'Create Account'}
            </Button>
          </form>
        </Card>

        {/* Footer Text */}
        <p className="text-center text-muted-foreground text-sm mt-6">
          {isLogin ? "Don't have an account?" : 'Already have an account?'}
          <button
            onClick={() => {
              setIsLogin(!isLogin)
              setError('')
              setSuccess('')
            }}
            className="ml-1 text-primary hover:underline font-medium"
          >
            {isLogin ? 'Sign up' : 'Sign in'}
          </button>
        </p>
      </div>
    </div>
  )
}
