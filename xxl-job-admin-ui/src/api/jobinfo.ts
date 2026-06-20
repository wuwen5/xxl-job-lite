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
    request.post<any, PageResult<JobInfo>>('/jobinfo/pageList', null, { params }),

  add: (data: Partial<JobInfo>) =>
    request.post<any, string>('/jobinfo', data),

  update: (id: number, data: Partial<JobInfo>) =>
    request.put<any, string>(`/jobinfo/${id}`, data),

  remove: (id: number) =>
    request.delete<any, string>(`/jobinfo/${id}`),

  start: (id: number) =>
    request.put<any, string>(`/jobinfo/start/${id}`),

  stop: (id: number) =>
    request.put<any, string>(`/jobinfo/stop/${id}`),

  trigger: (id: number, executorParam?: string, addressList?: string) =>
    request.post<any, string>('/jobinfo/trigger', null, {
      params: { id, executorParam, addressList }
    }),

  getNextTriggerTime: (scheduleType: string, scheduleConf: string) =>
    request.get<any, string[]>('/jobinfo/nextTriggerTime', {
      params: { scheduleType, scheduleConf }
    })
}
