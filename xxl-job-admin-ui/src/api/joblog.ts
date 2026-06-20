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
    request.post<any, PageResult<JobLog>>('/joblog/pageList', null, { params }),

  getJobsByGroup: (id: number) =>
    request.get<any, any[]>(`/joblog/getJobsByGroup/${id}`),

  getLogDetail: (logId: number, fromLineNum: number) =>
    request.get<any, LogResult>(`/joblog/logDetailCat/${logId}`, {
      params: { fromLineNum }
    }),

  kill: (id: number) =>
    request.post<any, string>('/joblog/logKill', null, { params: { id } }),

  clearLog: (jobGroup: number, jobId: number, type: number) =>
    request.post<any, string>('/joblog/clearLog', null, {
      params: { jobGroup, jobId, type }
    })
}
