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
    request.post<any, PageResult<JobGroup>>('/jobgroup/pageList', null, { params }),

  add: (data: Partial<JobGroup>) =>
    request.post<any, string>('/jobgroup', null, { params: data }),

  update: (id: number, data: Partial<JobGroup>) =>
    request.put<any, string>(`/jobgroup/${id}`, null, { params: data }),

  remove: (id: number) =>
    request.delete<any, string>(`/jobgroup/${id}`),

  getById: (id: number) =>
    request.get<any, JobGroup>(`/jobgroup/${id}`),

  getAll: () =>
    request.get<any, JobGroup[]>('/jobgroup/all')
}
