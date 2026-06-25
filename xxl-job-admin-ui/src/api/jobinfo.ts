import request from '@/utils/request'

export interface JobInfo {
  id: number
  jobGroup: number
  jobDesc: string
  author: string
  alarmEmail: string
  scheduleType: string
  scheduleConf: string
  misfireStrategy: string
  executorRouteStrategy: string
  executorHandler: string
  executorParam: string
  executorBlockStrategy: string
  executorTimeout: number
  executorFailRetryCount: number
  glueType: string
  glueSource: string
  glueRemark: string
  glueUpdatetime: string
  childJobId: string
  triggerStatus: number
  triggerLastTime: number
  triggerNextTime: number
}

export interface PageResult<T> {
  recordsTotal: number
  recordsFiltered: number
  data: T[]
}

export const jobinfoApi = {
  getPageList: (params: any) =>
    request.get<any, PageResult<JobInfo>>('/jobinfo', { params }),

  add: (data: Partial<JobInfo>) =>
    request.post<any, string>('/jobinfo', data),

  update: (id: number, data: Partial<JobInfo>) =>
    request.put<any, string>(`/jobinfo/${id}`, data),

  remove: (id: number) =>
    request.delete<any, string>(`/jobinfo/${id}`),

  start: (id: number) =>
    request.patch<any, string>(`/jobinfo/${id}`, null, { params: { action: 'start' } }),

  stop: (id: number) =>
    request.patch<any, string>(`/jobinfo/${id}`, null, { params: { action: 'stop' } }),

  trigger: (id: number, executorParam?: string, addressList?: string) =>
    request.post<any, string>(`/jobinfo/${id}/trigger`, null, {
      params: { executorParam, addressList }
    }),

  getNextTriggerTime: (scheduleType: string, scheduleConf: string) =>
    request.get<any, string[]>('/jobinfo/trigger-time/next', {
      params: { scheduleType, scheduleConf }
    })
}
