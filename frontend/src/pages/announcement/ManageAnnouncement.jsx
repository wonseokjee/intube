import * as React from 'react';
import List from '@mui/material/List';
import ListItem from '@mui/material/ListItem';
import ListItemButton from '@mui/material/ListItemButton';
import Typography from '@mui/material/Typography';
import Grid from '@mui/material/Grid';
import Divider from '@mui/material/Divider';
import getInterviewList from "json/interview_list";
import Button from '@mui/material/Button';
import { useNavigate } from "react-router-dom";
const newList = getInterviewList.interview_list.map((item) => {
  return item;
});

export default function ManageAnnouncement() {
  const navigate = useNavigate();
  function handlePage(e, interview) {
    let link="/"
    console.log(interview)
    if(interview.applicant_state==="0") link = "/announcement/recuit"
    else if(interview.applicant_state==="1")  link = "/announcement/progress"
    else if(interview.applicant_state==="2")  link = "/announcement/close"
    navigate(link, {state : interview});
  }
  return (
    <div>
    <Typography variant="h5" gutterBottom sx={{fontWeight:"bold", ml: 3, mt:3 }}>
    인터뷰 관리
    </Typography>
    <Button variant="outlined" sx={{ml: 2, my:1 }}>전체</Button>
    <Button variant="outlined" sx={{ml: 2, my:1 }}>진행중</Button>
    <Button variant="outlined" sx={{ml: 2, my:1 }}>모집중</Button>
    <Button variant="outlined" sx={{ml: 2, my:1 }}>완료</Button>
    <Divider/>
      <List>
          <ListItem disablePadding>
          <ListItemButton sx={{pl:0}} disabled>
                <Grid
        container
        alignItems="center"
        justifyContent="center"
        spacing={3}
      >
        <Grid item xs={2} sx={{ textAlign:"center" }}>
          <Typography variant="subtitle2">
            공고번호/등록일
          </Typography>
        </Grid>
        <Grid item xs={2} sx={{ textAlign:"center" }}>
          <Typography variant="subtitle2" gutterBottom>
            상태
          </Typography>
        </Grid>
        <Grid item xs={4} sx={{ textAlign:"center" }}>
        <Typography variant="subtitle2" gutterBottom>
            인터뷰공고
          </Typography>
        </Grid>
        <Grid item xs={2} sx={{  textAlign:"center" }}>
        <Typography variant="subtitle2" gutterBottom>
            지원자현황
          </Typography>
        </Grid>
        <Grid item xs={2} sx={{  textAlign:"center" }}>
        <Typography variant="subtitle2" gutterBottom>
            모집현황
          </Typography>
        </Grid>
      </Grid>
      </ListItemButton>
          </ListItem>
          <Divider/>
          {newList.map((interview) => (
          <div className='list-item' key={interview.id}>
          <ListItem disablePadding>
            <ListItemButton sx={{pl:0, pt:2}} onClick={(e) => handlePage(e, interview)}>
            <Grid
        container
        alignItems="center"
        justifyContent="center"
        spacing={3}
      >
        <Grid item xs={2} sx={{ textAlign:"center" }}>
          <Typography variant="subtitle2">
          {interview.id}
          </Typography>
          <Typography variant="caption">
          {interview.apply_start_time}
          </Typography>
        </Grid>
        <Grid item xs={2} sx={{ textAlign:"center" }}>
          <Typography variant="subtitle2" gutterBottom>
          {interview.applicant_state}
          </Typography>

        </Grid>
        <Grid item xs={4}>
        <Typography variant="subtitle1" gutterBottom>
        {interview.title}
          </Typography>
          <Typography variant="caption" gutterBottom>
        {interview.description}
          </Typography>
        </Grid>
        <Grid item xs={2}>
        <Typography variant="subtitle2" gutterBottom>
            조회수 : 12회
          </Typography>
          <Typography variant="subtitle2" gutterBottom>
            지원자 : 5명
          </Typography>
        </Grid>
        <Grid item xs={2}>
        <Typography variant="subtitle2" gutterBottom>
            예비합격 : 1명
          </Typography>
          <Typography variant="subtitle2" gutterBottom>
            불합격 : 1명
          </Typography>
        </Grid>
      </Grid>
            </ListItemButton>
          </ListItem>
          <Divider/>
          </div>))}
        </List>
    </div>
  );
}