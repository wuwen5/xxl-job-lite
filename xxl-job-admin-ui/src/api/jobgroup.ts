import request from '@/utils/request'

export interface JobGroup {
  id: number
  appname: string
  title: string
  addressType: number
  addressList: string
}

export interface PageResult<T> {
  recordsTotal: number
  recordsFiltered: number
  data: T[]
}

export const jobgroupApi = {
  getPageList: (params: any) =>
    request.get<any, PageResult<JobGroup>>('/jobgroup', { params }),

  add: (data: Partial<JobGroup>) =>
    request.post<any, string>('/jobgroup', data),

  update: (id: number, data: Partial<JobGroup>) =>
    request.put<any, string>(`/jobgroup/${id}`, data),

  remove: (id: number) =>
    request.delete<any, string>(`/jobgroup/${id}`),

  getById: (id: number) =>
    request.get<any, JobGroup>(`/jobgroup/${id}`),

  getAll: () =>
    request.get<any, JobGroup[]>('/jobgroup/all')
}
