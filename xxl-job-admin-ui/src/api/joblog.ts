import request from '@/utils/request'

export interface JobLog {
  id: number
  jobGroup: number
  jobId: number
  jobDesc: string
  executorAddress: string
  executorHandler: string
  executorParam: string
  triggerTime: string
  triggerCode: number
  triggerMsg: string
  handleTime: string
  handleCode: number
  handleMsg: string
}

export interface PageResult<T> {
  recordsTotal: number
  recordsFiltered: number
  data: T[]
}

export interface LogResult {
  fromLineNum: number
  toLineNum: number
  logContent: string
  end: boolean
}

export const joblogApi = {
  getPageList: (params: any) =>
    request.get<any, PageResult<JobLog>>('/joblog', { params }),

  getJobsByGroup: (id: number) =>
    request.get<any, any[]>(`/joblog/group/${id}/jobs`),

  getLogDetail: (logId: number, fromLineNum: number) =>
    request.get<any, LogResult>(`/joblog/${logId}/detail`, {
      params: { fromLineNum }
    }),

  kill: (id: number) =>
    request.post<any, string>(`/joblog/${id}/kill`),

  clearLog: (jobGroup: number, jobId: number, type: number) =>
    request.delete<any, string>('/joblog', {
      params: { jobGroup, jobId, type }
    })
}
