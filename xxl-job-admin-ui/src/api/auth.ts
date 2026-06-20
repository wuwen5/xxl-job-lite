import request from '@/utils/request'

export function loginApi(userName: string, password: string, ifRemember: boolean) {
  const formData = new FormData()
  formData.append('userName', userName)
  formData.append('password', password)
  formData.append('ifRemember', String(ifRemember))
  return request.post<any, string>('/login', formData)
}

export function logoutApi() {
  return request.post<any, string>('/logout')
}

export function getDashboardInfo() {
  return request.get<any, any>('/dashboard')
}

export function getChartInfo(startDate: string, endDate: string) {
  return request.get<any, any>('/chartInfo', { params: { startDate, endDate } })
}
