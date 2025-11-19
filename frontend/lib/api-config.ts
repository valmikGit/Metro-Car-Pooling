// API Configuration for Docker environment
export const API_BASE_URL = process.env.NEXT_PUBLIC_API_URL || 'http://localhost:8081'

export async function apiRequest(endpoint: string, options?: RequestInit) {
  const url = `${API_BASE_URL}${endpoint}`
  
  console.log('🔍 API Request:', {
    url,
    method: options?.method || 'GET',
    endpoint,
    baseUrl: API_BASE_URL
  })
  
  const token = typeof window !== 'undefined' ? localStorage.getItem('authToken') : null
  
  const headers: HeadersInit = {
    'Content-Type': 'application/json',
    ...(token && { 'Authorization': `Bearer ${token}` }),
    ...options?.headers,
  }

  try {
    const response = await fetch(url, {
      ...options,
      headers,
      mode: 'cors', // Explicitly set CORS mode
    })

    console.log('✅ API Response:', {
      status: response.status,
      statusText: response.statusText,
      ok: response.ok
    })

    return response
  } catch (error) {
    console.error('❌ API Request Failed:', {
      url,
      error: error instanceof Error ? error.message : error
    })
    throw error
  }
}