import axios from 'axios'
import { toast } from '@/utils/toast'

const request = axios.create({ baseURL: '/api', timeout: 15000 })

request.interceptors.request.use((config) => {
  const token = localStorage.getItem('admin_token')
  if (token) config.headers['user_info'] = token
  return config
}, (error) => Promise.reject(error))

request.interceptors.response.use(
  (response) => {
    const res = response.data
    if (res.code !== 200) {
      if (!response.config.noError) {
        toast.error(res.message || '请求失败')
      }
      if (res.code === 401) {
        localStorage.removeItem('admin_token')
        localStorage.removeItem('admin_info')
        window.location.href = '/login'
      }
      return Promise.reject(new Error(res.message || '请求失败'))
    }
    return res
  },
  (error) => {
    if (error.response?.status === 401) {
      localStorage.removeItem('admin_token')
      localStorage.removeItem('admin_info')
      window.location.href = '/login'
      return Promise.reject(error)
    }
    if (!error.config?.noError) {
      toast.error(error.message || '网络错误')
    }
    return Promise.reject(error)
  }
)

export default request
