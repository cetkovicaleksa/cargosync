import { PageMetadataDto } from './page-metadata.dto';

export interface PageDto<T> {
  content: T[];
  page: PageMetadataDto;
}
