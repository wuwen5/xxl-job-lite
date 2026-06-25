import request from '@/utils/request'

export interface JobUser {
  id: number
  username: string
  password: string
  role: number
  permission: string
}

export interface PageResult<T> {
  recordsTotal: number
  recordsFiltered: number
  data: T[]
}

export const userApi = {
  getPageList: (params: any) =>
    request.get<any, PageResult<JobUser>>('/user', { params }),

  add: (data: Partial<JobUser>) =>
    request.post<any, string>('/user', data),

  update: (id: number, data: Partial<JobUser>) =>
    request.put<any, string>(`/user/${id}`, data),

  remove: (id: number) =>
    request.delete<any, string>(`/user/${id}`),

  updatePwd: (password: string, oldPassword: string) =>
    request.put<any, string>('/user/me/password', { password, oldPassword })
}
